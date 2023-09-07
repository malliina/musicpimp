package com.malliina.musicpimp.messaging.cloud

import com.malliina.json.PrimitiveFormats
import com.malliina.push.wns._
import play.api.libs.json.{Format, Json, OFormat}

import scala.concurrent.duration.Duration

case class WNSRequest(tile: Option[TileElement],
                      toast: Option[ToastElement],
                      badge: Option[Badge],
                      raw: Option[Raw],
                      cache: Option[Boolean] = None,
                      ttl: Option[Duration] = None,
                      tag: Option[String]) {
  val payload: Option[WNSNotification] = tile orElse toast orElse badge orElse raw
  val message = payload.map(p => WNSMessage(p, cache, ttl, tag))
}

object WNSRequest {
  implicit val dur: Format[Duration] = PrimitiveFormats.durationFormat
  implicit val json: OFormat[WNSRequest] = Json.format[WNSRequest]
}
