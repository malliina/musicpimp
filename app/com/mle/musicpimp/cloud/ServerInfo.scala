package com.mle.musicpimp.cloud

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class ServerInfo(server: String, username: String, password: String)

object ServerInfo {
  implicit val json = Json.format[ServerInfo]
}