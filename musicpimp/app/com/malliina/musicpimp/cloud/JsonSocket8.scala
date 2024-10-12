package com.malliina.musicpimp.cloud

import com.malliina.http.FullUrl
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import javax.net.ssl.SSLSocketFactory
import scala.util.Try

class JsonSocket8(uri: FullUrl, socketFactory: SSLSocketFactory, headers: (String, String)*)
  extends Socket8[Json](uri, socketFactory, headers*):

  def sendMessage[T: Encoder](message: T): Try[Unit] =
    send(message.asJson)

  override protected def parse(raw: String): Option[Json] =
    io.circe.parser.parse(raw).toOption

  override protected def stringify(message: Json): String =
    message.noSpaces
