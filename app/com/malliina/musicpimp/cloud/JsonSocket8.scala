package com.malliina.musicpimp.cloud

import javax.net.ssl.SSLSocketFactory

import com.malliina.musicpimp.models.PimpUrl
import play.api.libs.json.{JsValue, Json, Writes}

import scala.util.Try

class JsonSocket8(uri: PimpUrl,
                  socketFactory: SSLSocketFactory,
                  headers: (String, String)*)
  extends Socket8[JsValue](uri, socketFactory, headers: _*) {

  def sendMessage[T: Writes](message: T): Try[Unit] =
    send(Json toJson message)

  override protected def parse(raw: String): Option[JsValue] =
    Try(Json parse raw).toOption

  override protected def stringify(message: JsValue): String =
    Json stringify message
}
