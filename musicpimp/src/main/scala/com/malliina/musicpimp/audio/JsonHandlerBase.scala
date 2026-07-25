package com.malliina.musicpimp.audio

import cats.effect.Sync
import com.malliina.musicpimp.audio.JsonHandlerBase.log
import com.malliina.musicpimp.auth.{AuthedRequest, JsonRequest}
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.http4s.Responses
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{CookiedRequest, FullUrls}
import com.malliina.values.Username
import io.circe.Json
import play.api.Logger

object JsonHandlerBase:
  private val log = Logger(getClass)

trait JsonHandlerBase[F[_]: Sync]:
  def fulfillMessage(message: PlayerMessage, request: RemoteInfo[F]): F[Unit]

  def onJson(req: JsonRequest[F]): F[Unit] =
    // Safe to use Target.noop because this method is called form POSTing which is write-only (in our case)
    val remoteInfo =
      RemoteInfo(
        req.username,
        Responses.apiVersion(req.request),
        FullUrls.hostOnly2(req.request),
        Target.noop
      )
    onJson(req.body, remoteInfo)

  /** Handles messages sent by web players.
    */
  def onJson(msg: Json, remote: RemoteInfo[F]): F[Unit] =
    log.info(s"User '${remote.user}' said: '$msg'.")
    handleMessage(msg, remote)

  def handleMessage(msg: Json, request: RemoteInfo[F]): F[Unit] =
    msg
      .as[PlayerMessage]
      .fold(
        err =>
          log.error(s"Invalid JSON: '$msg', error: $err.")
          Sync[F].raiseError(err)
        ,
        ok => fulfillMessage(ok, request)
      )
