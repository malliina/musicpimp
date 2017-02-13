package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.apns.{APNSMessage, APNSToken}
import play.api.libs.json.Json

case class APNSRequest(tokens: Seq[APNSToken], message: APNSMessage)
  extends PushRequest[APNSToken, APNSMessage]

object APNSRequest {
  implicit val json = Json.format[APNSRequest]
}
