package com.malliina.musicpimp.messaging.cloud

import org.asynchttpclient.Response
import play.api.libs.json.Json

case class BasicResult(statusCode: Int)

object BasicResult {
  implicit val json = Json.format[BasicResult]

  def fromResponse(response: Response) = BasicResult(response.getStatusCode)
}
