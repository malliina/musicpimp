package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import com.mle.util.Log
import controllers.WebPlayer
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json, Writes}

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
    WebPlayer.unicast(user, json)
  }
}