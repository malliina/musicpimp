package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.AndroidDevice
import com.malliina.push.adm.ADMToken
import play.api.libs.json.Json

/**
  * @author mle
  */
case class ADMDevice(id: ADMToken, tag: String) extends AndroidDevice[ADMToken]

object ADMDevice {
  implicit val json = Json.format[ADMDevice]
}
