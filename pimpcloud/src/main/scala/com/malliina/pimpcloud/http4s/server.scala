package com.malliina.pimpcloud.http4s

import cats.Parallel
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, IO, Resource}
import com.malliina.http.io.HttpClientIO
import com.malliina.http4s.{AppServer, ServerResources, StaticService}
import com.malliina.logback.{AppLogging, PimpAppender}
import com.malliina.musicpimp.auth.{Http4sAuth, JWT}
import com.malliina.pimpcloud.BuildInfo
import com.malliina.pimpcloud.auth.ProdAuth
import com.malliina.pimpcloud.html.CloudTags
import com.malliina.web.GoogleAuthFlow
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.server.{Router, Server}

trait CloudServerResources extends ServerResources:
  private val userAgent = s"pimpcloud/${BuildInfo.version} (${BuildInfo.gitHash.take(7)})"

  def app[F[_]: {Async, Files}](conf: CloudConf): Resource[F, Service[F]] =
    val jwt = JWT(conf.secret)
    val cookies = Http4sAuth[F](jwt)
    for
      dispatcher <- Dispatcher.parallel[F]
      appender <- PimpAppender.installF[F]
      http <- HttpClientIO.resource[F]
      _ <- AppLogging.resource("pimpcloud", userAgent, dispatcher, http)
      servers <- Resource.eval(Servers.default[F])
      auth = ProdAuth(servers, cookies)
      google = GoogleAuth(
        GoogleAuthFlow(conf.google, http),
        GoogleUris(Reverse),
        GoogleAuth.googleCookies,
        cookies
      )
      sockets <- Resource.eval(Sockets.default(auth, servers, google, appender))
    yield Service[F](sockets, CloudTags.default, auth, cookies, google)

  private def staticAssets[F[_]: {Async, Files}] = StaticService.paths[F](
    BuildInfo.assetsDir.toPath,
    BuildInfo.assetsPrefix,
    BuildInfo.isProd
  )

  def cloudServer[F[+_]: {Async, Files, Parallel, Compression, Network}](
    service: Service[F]
  ): Resource[F, Server] =
    emberServer[F](serverPort): b =>
      Router(
        "/" -> service.allRoutes(b),
        "/assets" -> staticAssets[F].routes
      )

object CloudServer extends AppServer with CloudServerResources:
  AppLogging.init()
  override def server: Resource[IO, Server] =
    for
      conf <- Resource.eval(CloudConf.parseF[IO])
      service <- app[IO](conf)
      s <- cloudServer[IO](service)
    yield s
