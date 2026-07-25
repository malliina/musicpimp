package com.malliina.musicpimp.http4s

import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, Concurrent, ExitCode, IO, IOApp}
import cats.{Monad, Parallel}
import ch.qos.logback.classic.Level
import com.comcast.ip4s.{Port, host, port}
import com.malliina.database.DoobieDatabase
import com.malliina.file.FileUtilities
import com.malliina.http.io.HttpClientIO
import com.malliina.logback.PimpAppender
import com.malliina.musicpimp.{BuildInfo, Tray}
import com.malliina.musicpimp.app.{AppMode, InitOptions, PimpConf}
import com.malliina.musicpimp.audio.{MusicPlayer, PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.auth.{AuthBundles, Authenticator, CookieAuthenticator, Http4sAuth, JWT, PimpAuthenticator, RememberMe}
import com.malliina.musicpimp.cloud.{CloudSocket, Clouds, Deps}
import com.malliina.musicpimp.db.{DatabaseLibrary, DatabaseStats, DoobieIndexer, DoobiePlaylists, DoobieTokenStore, DoobieUserManager, FullText, Indexer}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.log.PimpLog
import com.malliina.musicpimp.messaging.{CloudPushClient, TokenService}
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.{FileUtil, Sys}
import com.malliina.util.{AppLogger, Logging}
import com.malliina.values.{ErrorMessage, Readable}
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.server.{Router, Server}
import org.http4s.{Http, HttpRoutes, Request, Response}

import scala.concurrent.duration.{Duration, DurationInt}

trait ServerResources:
  private val log = AppLogger(getClass)

  given Readable[Port] =
    Readable.string.emap(s => Port.fromString(s).toRight(ErrorMessage(s"Not a port: '$s'.")))

  private val serverPort: Port =
    Sys.env.readOpt[Port]("SERVER_PORT").getOrElse(port"9000")

  private val tray = Tray.default()

  private def initApp[F[_]: Async](opts: InitOptions) = Async[F].delay:
    Logging.level = Level.INFO
    FileUtilities.init("musicpimp")
    java.nio.file.Files.createDirectories(FileUtil.pimpHomeDir)
    if opts.useTray then tray.installTray()
    val version = BuildInfo.version
    log.info(
      s"Starting MusicPimp $version, app dir: ${FileUtil.pimpHomeDir}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}, indexing ${opts.indexer}"
    )

  def appResources[F[+_]: { Async, Files, Parallel, Compression }](
    conf: PimpConf
  ): Resource[F, Service[F]] =
    val F = Async[F]
    for
      appender <- PimpAppender.installF[F]
      _ <- Resource.eval(initApp[F](conf.opts))
      http <- HttpClientIO.resource[F]
      dispatcher <- Dispatcher.parallel[F]
      db <- DoobieDatabase.init(conf.db)
      userManager <- Resource.eval(DoobieUserManager.withUser(db))
      player <- MusicPlayer.default[F](dispatcher)
      fileLibrary = Library()
      indexer <- Resource.eval(Indexer.default[F](fileLibrary, DoobieIndexer(db)))
      library = Library()
      lib = DatabaseLibrary(db, library)
      tokenService = TokenService(CloudPushClient.default(http))
      scheduler <- ScheduledPlaybackService.resource(player, lib, tokenService, dispatcher)
      alarmHandler = JsonHandler(player, scheduler)
      playlists = DoobiePlaylists(db)
      stats = DatabaseStats(db)
      statsPlayer = StatsPlayer(player, stats)
      messageHandler = PlaybackMessageHandler(player, fileLibrary, lib, statsPlayer)
      deps = Deps(playlists, userManager, messageHandler, lib, stats, scheduler, http, dispatcher)
      fullText = FullText(db)
      clouds <- Resource.eval(
        Clouds.prod(player, alarmHandler, deps, fullText, CloudSocket.prodUri)
      )
      _ <- (if conf.opts.cloud then clouds.events else Stream.empty).runInBackground
      _ <- (if conf.opts.indexer then indexer.events else Stream.empty).runInBackground
      _ <- if conf.opts.alarms then Resource.eval(F.delay(scheduler.init())) else Resource.unit[F]
      _ <- player.events.runInBackground
      _ <- statsPlayer.subscription.runInBackground
      _ <- Clouds.playerEventsToPimpcloud(player, clouds).runInBackground
    yield
      val jwt = JWT(conf.secret)
      val cookieManager = Http4sAuth[F](jwt)
      val tokenStore = DoobieTokenStore[F](db)
      val rememberMe = RememberMe[F](tokenStore, cookieManager)
      val auth = PimpAuthenticator(userManager, rememberMe)
      val compositeAuth = Authenticator.anyOne(
        CookieAuthenticator.default[F](cookieManager, auth),
        PimpAuthenticator.cookie(rememberMe)
      )
      val bundles = AuthBundles(cookieManager)
      val webAuth = bundles.redirecting(Reverse.login, compositeAuth)
      val html = PimpHtml.forApp(AppMode.fromBuild.isProd)
      Service[F](
        player,
        userManager,
        auth,
        webAuth,
        cookieManager,
        lib,
        fileLibrary,
        playlists,
        stats,
        statsPlayer,
        scheduler,
        indexer,
        fullText,
        messageHandler,
        clouds,
        alarmHandler,
        http,
        appender,
        dispatcher,
        html
      )

  extension [F[_]: Concurrent, O](s: Stream[F, O])
    def runInBackground: Resource[F, Unit] =
      Stream.emit(()).concurrently(s).compile.resource.lastOrError

  private def appResource[F[+_]: { Async, Files, Parallel, Compression }](
    service: Service[F],
    builder: WebSocketBuilder2[F]
  ): Http[F, F] =
    GZip[F, F]:
      HSTS:
        orNotFound:
          Router(
            "/" -> service.allRoutes(builder),
            "/assets" -> StaticService[F].routes
          )

  def emberServer[F[+_]: { Async, Files, Parallel, Compression, Network }](
    service: Service[F],
    port: Port = serverPort
  ): Resource[F, Server] =
    for
      _ = log.info(s"Binding on port $port using app version ${BuildInfo.gitHash}...")
      server <- EmberServerBuilder
        .default[F]
        .withHost(host"0.0.0.0")
        .withPort(port)
        .withHttpWebSocketApp(b => appResource(service, b))
        .withIdleTimeout(60.seconds)
        .withRequestHeaderReceiveTimeout(30.seconds)
        .withErrorHandler(ErrorHandler[F].partial)
        .withShutdownTimeout(1.millis)
        .build
    yield server

  private def orNotFound[F[_]: Monad](rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    Kleisli: req =>
      rs.run(req).getOrElseF(BasicApiService[F].notFound(s"Not found: ${req.method} ${req.uri}."))

object AppServer extends IOApp with ServerResources:
  override def runtimeConfig =
    super.runtimeConfig.copy(cpuStarvationCheckInitialDelay = Duration.Inf)

  override def run(args: List[String]): IO[ExitCode] =
    val server =
      for
        conf <- Resource.eval(PimpConf.parseF[IO])
        app <- appResources[IO](conf)
        server <- emberServer[IO](app)
      yield server
    server.use(_ => IO.never).as(ExitCode.Success)
