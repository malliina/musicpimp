package com.malliina.musicpimp.auth

import com.malliina.play.models.Username
import play.api.libs.json.{Json, OFormat}

case class DataUser(username: Username, passwordHash: String)

object DataUser {
  implicit val json: OFormat[DataUser] = Json.format[DataUser]
}
