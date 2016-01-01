package com.malliina.musicpimp.messaging.gcm

import com.malliina.musicpimp.messaging.AndroidDevice
import com.malliina.push.gcm.GCMToken
import play.api.libs.json.Json

/**
  * @author mle
  */
case class GCMDevice(id: GCMToken, tag: String) extends AndroidDevice[GCMToken]

object GCMDevice {
  implicit val json = Json.format[GCMDevice]
}
