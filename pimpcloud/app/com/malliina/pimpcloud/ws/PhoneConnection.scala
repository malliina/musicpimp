package com.malliina.pimpcloud.ws

import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.Version
import com.malliina.pimpcloud.json.JsonStrings.{StatusKey, VersionKey}
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import play.api.libs.json.{JsValue, Writes}

import scala.concurrent.Future

class PhoneConnection(val user: Username, val server: PimpServerSocket) {

  def pingAuth(): Future[Version] =
    server.proxied[Version](VersionKey, user)

  def status() = server.proxied[JsValue](StatusKey, user)

  def makeRequest[C: Writes](cmd: String, body: C): Future[JsValue] =
    server.defaultProxy(PhoneRequest(cmd, user, body))
}

object PhoneConnection {
  def apply(user: Username, server: PimpServerSocket) =
    new PhoneConnection(user, server)
}
