package com.malliina.pimpcloud.models

import com.malliina.pimpcloud.BuildInfo
import play.api.libs.json.Json

case class HealthResponse(name: String, version: String, hash: String)

object HealthResponse {
  implicit val json = Json.format[HealthResponse]

  val default = HealthResponse(BuildInfo.name, BuildInfo.version, BuildInfo.hash)
}
