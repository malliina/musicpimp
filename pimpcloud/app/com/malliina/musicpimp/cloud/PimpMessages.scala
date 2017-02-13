package com.malliina.musicpimp.cloud

import play.api.libs.json.Json

object PimpMessages extends BaseMessages {

  case class Version(version: String) extends PimpMessage

  case class Failure(reason: String) extends PimpMessage

  implicit val versionFormat = Json.format[Version]
}
