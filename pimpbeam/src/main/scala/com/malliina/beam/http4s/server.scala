package com.malliina.beam.http4s

import cats.{Parallel, effect}
import cats.effect.{Async, IO}
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import com.malliina.beam.{BeamConf, BuildInfo, DiscoGs, BeamState}
import com.malliina.http.io.HttpClientIO
import com.malliina.http4s.{AppServer, ServerResources, StaticService}
import com.malliina.logback.AppLogging
import com.malliina.musicpimp.auth.{Http4sAuth, JWT}
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.server.{Router, Server}

import java.nio.file.Paths

trait BeamServerResources extends ServerResources:
  private val userAgent = s"pimpbeam/${BuildInfo.version} (${BuildInfo.gitHash.take(7)})"

  def app[F[_]: { Async, Files }](conf: BeamConf): Resource[F, Service[F]] =
    for
      dispatcher <- Dispatcher.parallel[F]
      http <- HttpClientIO.resource[F]
      _ <- AppLogging.resource("pimpcloud", userAgent, dispatcher, http)
      players <- Resource.eval(BeamState.default[F])
    yield
      val cookies = Http4sAuth[F](JWT(conf.secret))
      Service[F](players, DiscoGs[F](http), cookies, conf)

  private def staticAssets[F[_]: { Async, Files }] = StaticService.paths[F](
    Paths.get("assets"),
    Paths.get("public"),
    "public",
    false
  )

  def beamServer[F[+_]: { Async, Files, Parallel, Compression, Network }](
    service: Service[F]
  ): Resource[F, Server] =
    emberServer[F](serverPort): b =>
      Router(
        "/" -> service.allRoutes(b),
        "/assets" -> staticAssets[F].routes
      )

object CloudServer extends AppServer with BeamServerResources:
  AppLogging.init()
  override def server: Resource[IO, Server] =
    for
      conf <- Resource.eval(BeamConf.parseF[IO])
      service <- app[IO](conf)
      s <- beamServer[IO](service)
    yield s
