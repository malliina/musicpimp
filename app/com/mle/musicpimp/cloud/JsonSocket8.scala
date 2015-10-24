package com.mle.musicpimp.cloud

import javax.net.ssl.SSLContext

import play.api.libs.json.{JsValue, Json, Writes}

import scala.util.Try

/**
 * @author mle
 */
class JsonSocket8(uri: String,
                  sslContext: SSLContext,
                  headers: (String, String)*)
  extends Socket8[JsValue](uri, sslContext, headers: _*) {

  def sendMessage[T](message: T)(implicit writer: Writes[T]): Try[Unit] = send(Json toJson message)

  override protected def parse(raw: String): Option[JsValue] = Try(Json parse raw).toOption

  override protected def stringify(message: JsValue): String = Json stringify message
}
