package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.values.Username
import play.api.libs.json.{JsValue, Json, Writes}

case class UserRequest(cmd: String, body: JsValue, request: RequestID, username: Username)

object UserRequest {
  implicit val json = Json.format[UserRequest]

  def simple(cmd: String, request: RequestID) =
    UserRequest(cmd, Json.obj(), request, PimpServerSocket.nobody)

  def apply[W: Writes](data: PhoneRequest[W], request: RequestID): UserRequest =
    UserRequest(data.cmd, Json.toJson(data.body), request, data.user)
}
