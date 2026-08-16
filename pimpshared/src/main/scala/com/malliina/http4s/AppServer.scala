package com.malliina.http4s

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Resource
import org.http4s.server.Server

import scala.concurrent.duration.Duration

trait AppServer extends IOApp:
  def server: Resource[IO, Server]

  override def runtimeConfig =
    super.runtimeConfig.copy(cpuStarvationCheckInitialDelay = Duration.Inf)

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
