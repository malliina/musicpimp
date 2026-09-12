package com.malliina.http4s

import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{Async, Concurrent, Resource}
import cats.{Monad, Parallel}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.musicpimp.util.Sys
import com.malliina.values.{ErrorMessage, Readable}
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.{Http, HttpApp, HttpRoutes}

import scala.concurrent.duration.DurationInt

trait ServerResources:
  given Readable[Port] =
    Readable.string.emap(s => Port.fromString(s).toRight(ErrorMessage(s"Not a port: '$s'.")))

  val serverPort: Port =
    Sys.env.readOpt[Port]("SERVER_PORT").getOrElse(port"9001")

  def emberServer[F[_]: {Async, Compression, Files, Network, Parallel}](port: Port)(
    appBuilder: WebSocketBuilder2[F] => HttpRoutes[F]
  ): Resource[F, Server] =
    for server <- EmberServerBuilder
        .default[F]
        .withHost(host"0.0.0.0")
        .withPort(port)
        .withHttpWebSocketApp(b => makeApp[F](appBuilder(b)))
        .withIdleTimeout(60.hours)
        .withRequestHeaderReceiveTimeout(30.seconds)
        .withErrorHandler(ErrorHandler[F].partial)
        .withShutdownTimeout(1.millis)
        .build
    yield server

  private def makeApp[F[_]: {Async, Files, Parallel, Compression}](
    routes: HttpRoutes[F]
  ): Http[F, F] =
    GZip[F, F]:
      HSTS:
        orNotFound:
          routes

  private def orNotFound[F[_]: Monad](rs: HttpRoutes[F]): HttpApp[F] =
    Kleisli: req =>
      rs.run(req).getOrElseF(BasicApiService[F].notFound(s"Not found: ${req.method} ${req.uri}."))

  extension [F[_]: Concurrent, O](s: Stream[F, O])
    def runInBackground: Resource[F, Unit] =
      Stream.emit(()).concurrently(s).compile.resource.lastOrError
