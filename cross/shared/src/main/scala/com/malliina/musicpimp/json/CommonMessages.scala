package com.malliina.musicpimp.json

import com.malliina.pimpcloud.SharedStrings.{Event, Ping, Pong}
import play.api.libs.json.JsObject
import play.api.libs.json.Json.{JsValueWrapper, obj}

object CommonMessages {
  val ping = event(Ping)
  val pong = event(Pong)

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    obj(Event -> eventType) ++ obj(valuePairs: _*)
}

trait CommonMessages {
  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    CommonMessages.event(eventType, valuePairs: _*)
}
