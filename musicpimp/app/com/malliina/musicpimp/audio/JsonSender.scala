package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonSender.log
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.Target
import com.malliina.play.models.Username
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json, Writes}

trait JsonSender {
  def user: Username

  def target: Target

  def sendPayload[C: Writes](c: C) = send(Json.toJson(c))

  private def send(json: JsValue) = {
    log debug s"Sending to web player user: '$user': '$json'."
    target send json
  }
}

object JsonSender {
  private val log = Logger(getClass)
}
