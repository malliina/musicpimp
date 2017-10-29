package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.Json


case class APNSInput(messages: Seq[APNSPayload])

//  extends PushRequest[APNSToken, APNSMessage]

object APNSInput {
  implicit val json = Json.format[APNSInput]
}
