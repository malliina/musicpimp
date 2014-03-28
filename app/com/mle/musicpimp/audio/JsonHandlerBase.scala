package com.mle.musicpimp.audio

import play.api.libs.json.JsValue
import com.mle.util.Log
import com.mle.play.RequestInfo
import com.mle.play.controllers.AuthRequest

/**
 *
 * @author Michael
 */

trait JsonHandlerBase extends Log {
  def withCmd[T](json: JsValue)(f: JsonCmd => T): T =
    f(new JsonCmd(json))

  def onJson(req: AuthRequest[JsValue]): Unit =
    onJson(req.body, RequestInfo(req.user, req))

  /**
   * Handles messages sent by web players.
   *
   */
  def onJson(msg: JsValue, req: RequestInfo): Unit = {
    val user = req.user
    log info s"User: $user from: ${req.request.remoteAddress} said: $msg"
    handleMessage(msg, user)
  }

  protected def handleMessage(msg: JsValue, user: String): Unit
}



