package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonHandlerBase.log
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{CookiedRequest, FullUrls}
import com.malliina.values.Username
import play.api.Logger
import play.api.libs.json.JsValue

trait JsonHandlerBase {

  def fulfillMessage(message: PlayerMessage, request: RemoteInfo): Unit

  def onJson(req: CookiedRequest[JsValue, Username]): Unit = {
    // Safe to use Target.noop because this method is called form POSTing which is write-only (in our case)
    val remoteInfo = RemoteInfo(req.user, PimpRequest.apiVersion(req.rh), FullUrls.hostOnly(req.rh), Target.noop)
    onJson(req.body, remoteInfo)
  }

  /** Handles messages sent by web players.
    */
  def onJson(msg: JsValue, remote: RemoteInfo): Unit = {
    log info s"User '${remote.user}' said: '$msg'."
    handleMessage(msg, remote)
  }

  def handleMessage(msg: JsValue, request: RemoteInfo): Unit = {
    msg.validate[PlayerMessage]
      .map(fulfillMessage(_, request))
      .recoverTotal(err => log error s"Invalid JSON: '$msg', error: $err.")
  }
}

object JsonHandlerBase {
  private val log = Logger(getClass)
}
