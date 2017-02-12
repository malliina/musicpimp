package com.malliina.musicpimp.messaging

import com.malliina.push.gcm.{GCMMessage, GCMToken}
import play.api.libs.json.Json

case class GCMRequest(tokens: Seq[GCMToken], message: GCMMessage)
  extends PushRequest[GCMToken, GCMMessage]

object GCMRequest {
  implicit val json = Json.format[GCMRequest]
}
