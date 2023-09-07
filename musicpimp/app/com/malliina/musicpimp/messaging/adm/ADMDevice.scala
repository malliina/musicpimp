package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.{ServerTag, TaggedDevice}
import com.malliina.push.adm.ADMToken
import play.api.libs.json.{Json, OFormat}

case class ADMDevice(id: ADMToken, tag: ServerTag) extends TaggedDevice[ADMToken]

object ADMDevice {
  implicit val json: OFormat[ADMDevice] = Json.format[ADMDevice]
}
