package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.json.Json._
import com.mle.musicpimp.actor.WebPlayerManager
import com.mle.musicpimp.actor.Messages.UserJson
import com.mle.util.Log

/**
 *
 * @author mle
 */
trait JsonSender extends Log {
  def user: String

  protected def sendCommand(cmd: String) =
    sendJson(CMD -> cmd)

  protected def sendCommand[T](cmd: String, value: T)(implicit tjs: Writes[T]) =
    sendJson(CMD -> cmd, VALUE -> toJson(value))

  private def sendJson(fields: (String, Json.JsValueWrapper)*) =
    send(obj(fields: _*))

  protected def send(json: JsValue) {
    log debug s"Sending to: $user: $json"
    WebPlayerManager.king ! UserJson(user, json)
  }
}