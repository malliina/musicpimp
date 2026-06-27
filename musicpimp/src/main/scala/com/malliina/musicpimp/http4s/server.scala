package com.malliina.musicpimp.http4s

import cats.data.Kleisli
import cats.{Monad, Parallel, effect}
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.effect.kernel.{Resource, Sync}
import cats.effect.std.Dispatcher
import com.comcast.ip4s.{Port, host, port}
import com.malliina.database.DoobieDatabase
import com.malliina.http.HttpClient
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.app.{AppMode, PimpConf}
import com.malliina.musicpimp.auth.{AuthBundles, Authenticator, CookieAuthenticator, Http4sAuth, JWT, PimpAuthenticator, PimpAuths, RememberMe}
import com.malliina.musicpimp.db.{DatabaseLibrary, DoobieTokenStore, DoobieUserManager, FullText}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.util.Sys
import com.malliina.util.AppLogger
import com.malliina.values.{ErrorMessage, Readable}
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.{Http, HttpRoutes, Request, Response}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.{Router, Server}

import scala.concurrent.duration.{Duration, DurationInt}

trait ServerResources:
  private val log = AppLogger(getClass)

  given Readable[Port] =
    Readable.string.emap(s => Port.fromString(s).toRight(ErrorMessage(s"Not a port: '$s'.")))

  private val serverPort: Port =
    Sys.env.readOpt[Port]("SERVER_PORT").getOrElse(port"9000")

  private def appResource[F[+_]: { Async, Files, Parallel, Compression }](
    conf: PimpConf
  ): Resource[F, Http[F, F]] =
    for
      http <- HttpClient.resource[F]()
      dispatcher <- Dispatcher.parallel[F]
      db <- DoobieDatabase.init(conf.db)
      userManager <- Resource.eval(DoobieUserManager.withUser(db))
    yield
      val jwt = JWT(conf.secret)
      val cookieManager = Http4sAuth[F](jwt)
      val tokenStore = DoobieTokenStore[F](db)
      val rememberMe = RememberMe[F](tokenStore, cookieManager)
      val auth = PimpAuthenticator(userManager, rememberMe)
      val fullText = FullText(db)
      val compositeAuth = Authenticator.anyOne(
        CookieAuthenticator.default[F](cookieManager, auth),
        PimpAuthenticator.cookie(rememberMe)
      )
      val bundles = AuthBundles(cookieManager)
      val webAuth = bundles.redirecting(Reverse.login, compositeAuth)
      val library = Library()
      val lib = DatabaseLibrary(db, library)
      val html = PimpHtml.forApp(AppMode.fromBuild.isProd)
      GZip[F, F]:
        HSTS:
          orNotFound:
            Router(
              "/" -> Service[F](userManager, auth, webAuth, cookieManager, lib, html).routes,
              "/assets" -> StaticService[F].routes
            )

  def emberServer[F[+_]: { Async, Files, Parallel, Compression, Network }](
    conf: PimpConf
  ): Resource[F, Server] =
    for
      app <- appResource(conf)
      _ = log.info(s"Binding on port $serverPort using app version ${BuildInfo.gitHash}...")
      server <- EmberServerBuilder
        .default[F]
        .withHost(host"0.0.0.0")
        .withPort(serverPort)
        .withHttpApp(app)
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
        server <- emberServer[IO](conf)
      yield server
    server.use(_ => IO.never).as(ExitCode.Success)
