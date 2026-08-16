package com.malliina.pimpcloud.ws

import cats.effect.Async
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{GetMeta, PimpServerSocket}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.json.JsonStrings.{Meta, StatusKey}
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.AuthInfo
import com.malliina.values.Username
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Request

class PhoneConnection[F[_]: Async](
  val user: Username,
  val rh: Request[?],
  val server: PimpServerSocket[F]
) extends AuthInfo:
  def meta(id: TrackID): F[Decoder.Result[Track]] =
    val req = PhoneRequest(Meta, user, GetMeta(id))
    server.proxyValidated[GetMeta, Track](req)

  def status(): F[Json] = jsonRequest(StatusKey, Json.obj())

  def request(cmd: String): F[Json] = jsonRequest(cmd, Json.obj())

  def jsonRequest[C: Encoder](cmd: String, body: C): F[Json] =
    server.defaultProxy(PhoneRequest(cmd, user, body))
