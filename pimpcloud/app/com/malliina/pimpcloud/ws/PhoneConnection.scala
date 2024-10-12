package com.malliina.pimpcloud.ws

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{GetMeta, PimpServerSocket}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.json.JsonStrings.{Meta, StatusKey}
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.AuthInfo
import com.malliina.values.Username
import io.circe.{Decoder, Encoder, Json}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class PhoneConnection(val user: Username, val rh: RequestHeader, val server: PimpServerSocket)
  extends AuthInfo:
  def meta(id: TrackID): Future[Decoder.Result[Track]] =
    val req = PhoneRequest(Meta, user, GetMeta(id))
    server.proxyValidated[GetMeta, Track](req)

  def status(): Future[Json] = makeRequest(StatusKey, Json.obj())

  def makeRequest[C: Encoder](cmd: String, body: C): Future[Json] =
    server.defaultProxy(PhoneRequest(cmd, user, body))

object PhoneConnection:
  def apply(user: Username, rh: RequestHeader, server: PimpServerSocket): PhoneConnection =
    new PhoneConnection(user, rh, server)
