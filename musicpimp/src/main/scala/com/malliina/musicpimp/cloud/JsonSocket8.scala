package com.malliina.musicpimp.cloud

import cats.effect.{Async, Deferred, Resource}
import cats.effect.std.Dispatcher
import com.malliina.http.FullUrl
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import javax.net.ssl.SSLSocketFactory
import scala.util.Try

object JsonSocket8:
  def default[F[_]: Async](
    uri: FullUrl,
    socketFactory: SSLSocketFactory,
    headers: (String, String)*
  ): Resource[F, JsonSocket8[F]] =
    for
      connect <- Resource.eval(Deferred[F, Option[Throwable]])
      d <- Dispatcher.parallel[F]
    yield JsonSocket8(uri, connect, d, socketFactory, headers*)

class JsonSocket8[F[_]: Async](
  uri: FullUrl,
  connectPromise: Deferred[F, Option[Throwable]],
  d: Dispatcher[F],
  socketFactory: SSLSocketFactory,
  headers: (String, String)*
) extends Socket8[F, Json](uri, connectPromise, d, socketFactory, headers*):

  def sendMessage[T: Encoder](message: T): Try[Unit] =
    send(message.asJson)

  override protected def parse(raw: String): Option[Json] =
    io.circe.parser.parse(raw).toOption

  override protected def stringify(message: Json): String =
    message.noSpaces
