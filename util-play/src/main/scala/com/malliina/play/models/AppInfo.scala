package com.malliina.play.models

import play.api.libs.json.{Json, OFormat}

case class AppInfo(name: String, version: String, hash: String)

object AppInfo {
  implicit val json: OFormat[AppInfo] = Json.format[AppInfo]
}
