package com.malliina.musicpimp.messaging.cloud

import io.circe.Codec
import play.api.libs.json.{Json, OFormat}

case class WNSResult(reason: String, description: String, statusCode: Int, isSuccess: Boolean)

object WNSResult {
  implicit val json: Codec[WNSResult] = Codec.derived[WNSResult]
}
