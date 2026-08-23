package com.malliina.logback

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.effect.{Async, IO}
import com.malliina.logback.fs2.{DefaultFS2IOAppender, FS2AppenderComps, LoggingComps}
import com.malliina.logstreams.client.FS2Appender

class PimpAppender extends DefaultFS2IOAppender[IO](FS2Appender.unsafe.comps)

object PimpAppender:
  val name = "AKKA"

  def install(): Unit = installAppender(PimpAppender())

  private def installAppender[F[_]: Async](appender: DefaultFS2IOAppender[F]): Unit =
    appender.setContext(LogbackUtils.loggerContext)
    appender.setName(name)
    appender.setTimeFormat("yyyy-MM-dd HH:mm:ss")
    LogbackUtils.installAppender(appender)

  def installF[F[_]: Async]: Resource[F, DefaultFS2IOAppender[F]] =
    for
      deps <- comps[F]
      appender = DefaultFS2IOAppender[F](deps)
      _ <- Resource.eval(Async[F].delay(installAppender(appender)))
    yield appender

  def comps[F[_]: Async]: Resource[F, LoggingComps[F]] =
    for
      d <- Dispatcher.parallel[F]
      comps <- Resource.eval(FS2AppenderComps.io(d))
    yield comps
