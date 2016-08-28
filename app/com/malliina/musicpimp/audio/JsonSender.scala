package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonSender.log
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.User
import controllers.WebPlayer
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json, Writes}

trait JsonSender {
  def user: User

  def webPlayer: WebPlayer

  protected def sendCommand(cmd: String) =
    sendJson(Cmd -> cmd)

  protected def sendCommand[T: Writes](cmd: String, value: T) =
    sendJson(Cmd -> cmd, Value -> toJson(value))

  private def sendJson(fields: (String, Json.JsValueWrapper)*) =
    send(obj(fields: _*))

  protected def send(json: JsValue) = {
    log info s"Sending to web player user: $user: $json"
    webPlayer.unicast(user, json)
  }
}

object JsonSender {
  private val log = Logger(getClass)
}
