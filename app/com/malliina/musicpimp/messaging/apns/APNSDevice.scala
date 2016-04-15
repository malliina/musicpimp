package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.{ServerTag, TaggedDevice}
import com.malliina.push.apns.APNSToken
import play.api.libs.json.Json

case class APNSDevice(id: APNSToken, tag: ServerTag) extends TaggedDevice[APNSToken]

object APNSDevice {
  implicit val json = Json.format[APNSDevice]
}
