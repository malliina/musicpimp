package com.malliina.beam

import com.malliina.play.BeamStrings._
import com.malliina.play.json.JsonStrings.{Cmd, Event}
import com.malliina.values.Username
import play.api.libs.json.JsObject
import play.api.libs.json.Json._

trait BeamMessages {
  val reset = obj(Cmd -> RESET)
  val version = obj(VERSION -> BuildInfo.version)

  def playerExists(user: Username, exists: Boolean, ready: Boolean) =
    obj(USER -> user, EXISTS -> exists, READY -> ready)

  def partyDisconnected(user: Username) =
    event(DISCONNECTED, USER -> user)

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    obj(Event -> eventType) ++ obj(valuePairs: _*)

  def coverAvailable = event(COVER_AVAILABLE, COVER_SOURCE -> COVER_RESOURCE)
}

object BeamMessages extends BeamMessages
