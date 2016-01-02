package com.malliina.musicpimp.messaging.cloud

import com.ning.http.client.Response
import play.api.libs.json.Json

/**
  * @author mle
  */
case class BasicResult(statusCode: Int)

object BasicResult {
  implicit val json = Json.format[BasicResult]

  def fromResponse(response: Response) = BasicResult(response.getStatusCode)
}
