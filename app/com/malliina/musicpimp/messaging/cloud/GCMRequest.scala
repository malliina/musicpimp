package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMMessage, GCMToken}
import play.api.libs.json.Json

/**
  * @author mle
  */
case class GCMRequest(tokens: Seq[GCMToken], message: GCMMessage)
  extends PushRequest[GCMToken, GCMMessage]

object GCMRequest {
  implicit val json = Json.format[GCMRequest]
}
