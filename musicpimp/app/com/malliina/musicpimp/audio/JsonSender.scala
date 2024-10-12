package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonSender.log
import com.malliina.musicpimp.json.Target
import com.malliina.values.Username
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import play.api.Logger

trait JsonSender:
  def user: Username
  def target: Target
  def sendPayload[C: Encoder](c: C): Unit = send(c.asJson)

  private def send(json: Json) =
    log.debug(s"Sending to web player user: '$user': '$json'.")
    target.send(json)

object JsonSender:
  private val log = Logger(getClass)
