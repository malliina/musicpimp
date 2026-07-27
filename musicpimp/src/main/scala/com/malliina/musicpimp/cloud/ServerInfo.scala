package com.malliina.musicpimp.cloud

import play.api.libs.json.{Json, OFormat}

/** @author
  *   Michael
  */
case class ServerInfo(server: String, username: String, password: String)

object ServerInfo:
  implicit val json: OFormat[ServerInfo] = Json.format[ServerInfo]
