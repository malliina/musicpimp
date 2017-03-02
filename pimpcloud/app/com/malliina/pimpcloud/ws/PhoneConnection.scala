package com.malliina.pimpcloud.ws

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{GetMeta, PimpServerSocket}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.json.JsonStrings.{Meta, StatusKey}
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import play.api.libs.json.{JsResult, JsValue, Json, Writes}

import scala.concurrent.Future

class PhoneConnection(val user: Username, val server: PimpServerSocket) {
  def meta(id: TrackID): Future[JsResult[Track]] = {
    val req = PhoneRequest(Meta, user, GetMeta(id))
    server.proxyValidated[GetMeta, Track](req)
  }

  def status() = makeRequest(StatusKey, Json.obj())

  def makeRequest[C: Writes](cmd: String, body: C): Future[JsValue] =
    server.defaultProxy(PhoneRequest(cmd, user, body))
}

object PhoneConnection {
  def apply(user: Username, server: PimpServerSocket) =
    new PhoneConnection(user, server)
}
