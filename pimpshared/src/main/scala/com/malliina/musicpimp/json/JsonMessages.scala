package com.malliina.musicpimp.json

import com.malliina.musicpimp.json.JsonStrings.{AccessDenied, Event, Ping, Reason, Welcome}
import io.circe.Json
import io.circe.syntax.EncoderOps

trait JsonMessages:
  def failure(reason: String) = Json.obj(Reason -> reason.asJson)

  val ping = Json.obj(Event -> Ping.asJson)
  val welcome = Json.obj(Event -> Welcome.asJson)
  val unauthorized = failure(AccessDenied)

object JsonMessages extends JsonMessages
