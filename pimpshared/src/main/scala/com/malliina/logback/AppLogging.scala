package com.malliina.logback

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import ch.qos.logback.classic.Level
import com.malliina.http.HttpClient
import com.malliina.logstreams.client.LogstreamsUtils

object AppLogging:
  def init(): Unit =
    LogbackUtils.init(
      levelsByLogger = Map(
        "org.http4s.ember.server.EmberServerBuilderCompanionPlatform" -> Level.OFF
      )
    )

  def resource[F[_]: Async](
    appName: String,
    userAgent: String,
    d: Dispatcher[F],
    http: HttpClient[F]
  ): Resource[F, Boolean] =
    Resource.make(LogstreamsUtils.installIfEnabled(appName, userAgent, d, http))(_ =>
      Sync[F].delay(LogbackUtils.loggerContext.stop())
    )
