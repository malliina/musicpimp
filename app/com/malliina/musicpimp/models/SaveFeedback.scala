package com.malliina.musicpimp.models

import play.api.http.Writeable
import play.api.libs.json.Json

/**
  * @author mle
  */
case class SaveFeedback(created: Long)

object SaveFeedback {
  implicit val json = Json.format[SaveFeedback]
  implicit val w = Writeable.writeableOf_JsValue.map[SaveFeedback](sf => Json.toJson(sf))
}
