package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.RequestID
import play.api.libs.json.{JsValue, Json, OFormat}

case class BodyAndId(body: JsValue, request: RequestID)

object BodyAndId {
  implicit val json: OFormat[BodyAndId] = Json.format[BodyAndId]
}

class RequestFailure(val response: JsValue) extends Exception
