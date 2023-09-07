package com.malliina.musicpimp.models

import play.api.http.Writeable
import play.api.libs.json.{Json, OFormat}

case class SaveFeedback(created: Long)

object SaveFeedback {
  implicit val json: OFormat[SaveFeedback] = Json.format[SaveFeedback]
  implicit val w: Writeable[SaveFeedback] =
    Writeable.writeableOf_JsValue.map[SaveFeedback](sf => Json.toJson(sf))
}
