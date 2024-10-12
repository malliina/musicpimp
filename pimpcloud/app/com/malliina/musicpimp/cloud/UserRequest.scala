package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.values.Username
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Encoder, Json}

case class UserRequest(cmd: String, body: Json, request: RequestID, username: Username)
  derives Codec.AsObject

object UserRequest:
  def simple(cmd: String, request: RequestID) =
    UserRequest(cmd, Json.obj(), request, PimpServerSocket.nobody)

  def apply[W: Encoder](data: PhoneRequest[W], request: RequestID): UserRequest =
    UserRequest(data.cmd, data.body.asJson, request, data.user)
