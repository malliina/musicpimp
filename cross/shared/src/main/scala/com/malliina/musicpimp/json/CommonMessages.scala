package com.malliina.musicpimp.json

import com.malliina.musicpimp.js.FrontStrings.EventKey
import play.api.libs.json.JsObject
import play.api.libs.json.Json.{JsValueWrapper, obj}

trait CommonMessages {
  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    obj(EventKey -> eventType) ++ obj(valuePairs: _*)
}
