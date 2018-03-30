package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.HttpResponse
import play.api.libs.json.Json

case class BasicResult(statusCode: Int)

object BasicResult {
  implicit val json = Json.format[BasicResult]

  def fromResponse(response: HttpResponse) = BasicResult(response.code)
}
