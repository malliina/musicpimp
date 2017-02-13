package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.Json

case class WNSResult(reason: String,
                     description: String,
                     statusCode: Int,
                     isSuccess: Boolean)

object WNSResult {
  implicit val json = Json.format[WNSResult]
}
