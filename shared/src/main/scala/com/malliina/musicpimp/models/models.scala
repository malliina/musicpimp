package com.malliina.musicpimp.models

import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.play.Writeables
import play.api.libs.json.Json

case class Version(version: String) extends PimpMessage

object Version {
  implicit val json = Json.format[Version]
  implicit val html = Writeables.fromJson[Version]
}

case class FailReason(reason: String) extends PimpMessage

object FailReason {
  implicit val json = Json.format[FailReason]
  implicit val html = Writeables.fromJson[FailReason]
}
