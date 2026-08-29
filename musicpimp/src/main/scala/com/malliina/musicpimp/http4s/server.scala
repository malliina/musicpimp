package com.malliina.musicpimp.http4s

import cats.Parallel
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, IO}
import ch.qos.logback.classic.Level
import com.comcast.ip4s.Port
import com.malliina.database.DoobieDatabase
import com.malliina.file.FileUtilities
import com.malliina.http.io.HttpClientIO
import com.malliina.http4s.{AppServer, ServerResources}
import com.malliina.logback.{LogbackUtils, PimpAppender}
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
import com.malliina.musicpimp.util.FileUtil
import com.malliina.musicpimp.{BuildInfo, Tray}
import com.malliina.util.{AppLogger, Logging}
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.server.{Router, Server}

trait PimpServerResources extends ServerResources:
  private val log = AppLogger(getClass)

  private val tray = Tray.default()

  private def initApp[F[_]: Async](opts: InitOptions) = Async[F].delay:
    Logging.level = Level.INFO
    FileUtilities.init("musicpimp")
    java.nio.file.Files.createDirectories(FileUtil.pimpHomeDir)
    if opts.useTray then tray.installTray()
    val version = BuildInfo.version
    log.info(
      s"Starting MusicPimp $version, app dir: ${FileUtil.pimpHomeDir}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}, indexing ${opts.indexer}, clouds ${opts.cloud}"
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

  def pimpServer[F[+_]: { Async, Files, Parallel, Compression, Network }](
    service: Service[F],
    port: Port = serverPort
  ): Resource[F, Server] =
    log.info(s"Binding on port $port using app version ${BuildInfo.gitHash}...")
    emberServer[F](port): b =>
      Router(
        "/" -> service.allRoutes(b),
        "/assets" -> StaticService[F].routes
      )

object PimpServer extends AppServer with PimpServerResources:
  LogbackUtils.init(
    levelsByLogger = Map(
      "org.http4s.ember.server.EmberServerBuilderCompanionPlatform" -> Level.OFF,
      "org.jaudiotagger" -> Level.WARN
    )
  )

  override def server: Resource[IO, Server] =
    for
      conf <- Resource.eval(PimpConf.parseF[IO])
      app <- appResources[IO](conf)
      s <- pimpServer[IO](app)
    yield s
