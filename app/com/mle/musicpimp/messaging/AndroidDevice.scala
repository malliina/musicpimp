package com.mle.musicpimp.messaging

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class AndroidDevice(id: String, tag: String)

object AndroidDevice {
  implicit val json = Json.format[AndroidDevice]
}
