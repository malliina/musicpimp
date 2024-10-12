package com.malliina.beam

import com.malliina.play.BeamStrings.*
import com.malliina.play.json.JsonStrings.{Cmd, Event}
import com.malliina.values.Username
import io.circe.Json
import io.circe.syntax.EncoderOps

trait BeamMessages:
  val reset = Json.obj(Cmd -> RESET.asJson)

  def playerExists(user: Username, exists: Boolean, ready: Boolean) =
    Json.obj(USER -> user.name.asJson, EXISTS -> exists.asJson, READY -> ready.asJson)

  def partyDisconnected(user: Username) =
    event(DISCONNECTED, USER -> user.name.asJson)

  def event(eventType: String, valuePairs: (String, Json)*): Json =
    Json.obj(Event -> eventType.asJson).deepMerge(Json.obj(valuePairs*))

  def coverAvailable = event(COVER_AVAILABLE, COVER_SOURCE -> COVER_RESOURCE.asJson)

object BeamMessages extends BeamMessages
