package com.malliina.musicpimp.messaging.cloud

import com.malliina.json.JsonFormats
import com.malliina.push.wns._
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class WNSRequest(tokens: Seq[WNSToken],
                      tile: Option[TileElement],
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
  implicit val dur = JsonFormats.durationFormat
  implicit val json = Json.format[WNSRequest]
}
