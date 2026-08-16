package com.malliina.musicpimp.cloud

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.auth.Proxies2
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.*
import com.malliina.pimpcloud.json.JsonStrings.*
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.pimpcloud.ws.NoCacheByteStreams
import com.malliina.play.ContentRange
import com.malliina.values.Literals.user
import com.malliina.values.{Password, Username}
import com.malliina.ws.{JsonFutureSocket, Streamer}
import io.circe.{Decoder, Json}
import org.http4s.{Request, Response}

object PimpServerSocket:
  val DefaultSearchLimit = 100
  val nobody = user"nobody"

/** @param jsonOut
  *   send messages to this actor to send messages to the server
  * @param id
  *   the cloud ID of the server
  */
class PimpServerSocket[F[_]: Async](
  val jsonOut: Target[F],
  id: CloudID,
  val headers: Request[?],
  onUpdate: F[Unit]
) extends JsonFutureSocket[F](id):
  val address = Proxies2.realAddress(headers)
  val fileTransfers: Streamer[F] = NoCacheByteStreams(id, jsonOut, onUpdate)

  override def send(payload: Json): F[Boolean] = jsonOut.send(payload).as(true)

  def requestTrack(track: Track, contentRange: ContentRange, req: Request[?]): F[Response[F]] =
    fileTransfers.requestTrack(track, contentRange, req)

  /** @param user
    *   username
    * @param pass
    *   password
    * @return
    *   true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): F[Boolean] =
    authenticateWithVersion(user, pass)
      .as(true)
      .handleError(_ => false)

  private def authenticateWithVersion(user: Username, pass: Password): F[Decoder.Result[Version]] =
    val req = PhoneRequest(AuthenticateKey, PimpServerSocket.nobody, Authenticate(user, pass))
    proxyValidated[Authenticate, Version](req)
