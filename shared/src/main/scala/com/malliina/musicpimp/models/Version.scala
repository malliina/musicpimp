package com.malliina.musicpimp.models

import com.malliina.musicpimp.cloud.PimpMessage
import play.api.http.Writeable
import play.api.libs.json.Json

case class Version(version: String) extends PimpMessage

object Version {
  implicit val json = Json.format[Version]
  implicit val html = Writeable.writeableOf_JsValue.map[Version] { version =>
    Json.toJson(version)
  }
}

case class FailReason(reason: String) extends PimpMessage

object FailReason {
  implicit val json = Json.format[FailReason]
  implicit val html = Writeable.writeableOf_JsValue.map[FailReason] { reason =>
    Json.toJson(reason)
  }
}
