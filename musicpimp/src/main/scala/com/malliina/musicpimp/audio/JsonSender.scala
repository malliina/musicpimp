package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.JsonSender.log
import com.malliina.musicpimp.json.Target
import com.malliina.util.AppLogger
import com.malliina.values.Username
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps

trait JsonSender[F[_]]:
  def user: Username
  def target: Target[F]
  def sendPayload[C: Encoder](c: C): F[Unit] = send(c.asJson)

  private def send(json: Json) =
    log.debug(s"Sending to web player user: '$user': '$json'.")
    target.send(json)

object JsonSender:
  private val log = AppLogger(getClass)
