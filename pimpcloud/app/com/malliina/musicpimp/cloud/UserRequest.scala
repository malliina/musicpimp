package com.malliina.musicpimp.cloud

import java.util.UUID

import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import play.api.libs.json.{JsValue, Json}

case class UserRequest(cmd: String, body: JsValue, request: UUID, username: Username)

object UserRequest {
  implicit val json = Json.format[UserRequest]

  def apply(data: PhoneRequest, request: UUID, username: Username): UserRequest =
    UserRequest(data.cmd, data.body, request, username)
}
