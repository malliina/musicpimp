package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.apns.{APNSMessage, APSPayload}
import play.api.libs.json.Json.toJson

class APNSBuilder {
  val Message = "Open to stop"

  def buildRequest(dest: APNSDevice): APNSPayload = {
    val payload = APSPayload(Some(Left(Message)))
    val extra = Map(Cmd -> toJson(Stop), Tag -> toJson(dest.tag))
    val message = APNSMessage(payload, extra)
    APNSPayload(dest.id, message)
  }
}
