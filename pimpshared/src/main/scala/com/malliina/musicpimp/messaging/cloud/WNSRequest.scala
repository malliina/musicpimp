package com.malliina.musicpimp.messaging.cloud

import com.malliina.json.PrimitiveFormats.durationCodec
import com.malliina.push.wns.*
import io.circe.Codec

import scala.concurrent.duration.Duration

case class WNSRequest(
  tile: Option[TileElement],
  toast: Option[ToastElement],
  badge: Option[Badge],
  raw: Option[Raw],
  cache: Option[Boolean] = None,
  ttl: Option[Duration] = None,
  tag: Option[String]
) derives Codec.AsObject:
  val payload: Option[WNSNotification] = tile.orElse(toast).orElse(badge).orElse(raw)
  val message = payload.map(p => WNSMessage(p, cache, ttl, tag))
