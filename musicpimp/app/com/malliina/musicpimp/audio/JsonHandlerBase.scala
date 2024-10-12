package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonHandlerBase.log
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{CookiedRequest, FullUrls}
import com.malliina.values.Username
import io.circe.Json
import play.api.Logger

trait JsonHandlerBase:

  def fulfillMessage(message: PlayerMessage, request: RemoteInfo): Unit

  def onJson(req: CookiedRequest[Json, Username]): Unit =
    // Safe to use Target.noop because this method is called form POSTing which is write-only (in our case)
    val remoteInfo =
      RemoteInfo(req.user, PimpRequest.apiVersion(req.rh), FullUrls.hostOnly(req.rh), Target.noop)
    onJson(req.body, remoteInfo)

  /** Handles messages sent by web players.
    */
  def onJson(msg: Json, remote: RemoteInfo): Unit =
    log info s"User '${remote.user}' said: '$msg'."
    handleMessage(msg, remote)

  def handleMessage(msg: Json, request: RemoteInfo): Unit =
    msg
      .as[PlayerMessage]
      .map(fulfillMessage(_, request))
      .left
      .map: err =>
        log.error(s"Invalid JSON: '$msg', error: $err.")

object JsonHandlerBase:
  private val log = Logger(getClass)
