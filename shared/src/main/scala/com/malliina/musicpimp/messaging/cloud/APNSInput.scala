package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.{Json, OFormat}

case class APNSInput(messages: Seq[APNSPayload])

object APNSInput {
  implicit val json: OFormat[APNSInput] = Json.format[APNSInput]
}
