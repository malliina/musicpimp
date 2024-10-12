package com.malliina.musicpimp.json

import com.malliina.pimpcloud.SharedStrings.{Event, Ping, Pong}
import io.circe.Json
import io.circe.syntax.EncoderOps

object CommonMessages:
  val ping = event(Ping)
  val pong = event(Pong)

  def event(eventType: String, valuePairs: (String, Json)*): Json =
    Json.obj(Event -> eventType.asJson).deepMerge(Json.obj(valuePairs*))

trait CommonMessages:
  def event(eventType: String, valuePairs: (String, Json)*): Json =
    CommonMessages.event(eventType, valuePairs*)
