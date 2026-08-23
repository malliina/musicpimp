package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.HttpResponse
import io.circe.Codec

case class BasicResult(statusCode: Int)

object BasicResult:
  implicit val json: Codec[BasicResult] = Codec.derived[BasicResult]

  def fromResponse(response: HttpResponse) = BasicResult(response.code)
