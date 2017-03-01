package com.malliina.musicpimp.cloud

import java.util.UUID

import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import play.api.libs.json.{JsValue, Json, Writes}

case class UserRequest(cmd: String, body: JsValue, request: UUID, username: Username)

object UserRequest {
  implicit val json = Json.format[UserRequest]

  def apply[W: Writes](data: PhoneRequest[W], request: UUID): UserRequest =
    UserRequest(data.cmd, Json.toJson(data.body), request, data.user)
}
