package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.{ServerTag, TaggedDevice}
import com.malliina.push.adm.ADMToken
import play.api.libs.json.Json

case class ADMDevice(id: ADMToken, tag: ServerTag) extends TaggedDevice[ADMToken]

object ADMDevice {
  implicit val json = Json.format[ADMDevice]
}
