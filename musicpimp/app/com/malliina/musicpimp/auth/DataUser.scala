package com.malliina.musicpimp.auth

import com.malliina.values.Username
import play.api.libs.json.{Json, OFormat}

case class DataUser(user: Username, passHash: String)

object DataUser {
  implicit val json: OFormat[DataUser] = Json.format[DataUser]
}
