package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.WindowsMessage
import com.malliina.push.mpns._
import play.api.libs.json.Json

case class MPNSRequest(tokens: Seq[MPNSToken],
                       toast: Option[ToastMessage] = None,
                       tile: Option[TileData] = None,
                       flip: Option[FlipData] = None,
                       iconic: Option[IconicData] = None,
                       cycle: Option[CycleTile] = None) {
  val message: Option[WindowsMessage] =
    toast orElse tile orElse flip orElse iconic orElse cycle
}

object MPNSRequest {
  implicit val tile = Json.format[TileData]
  implicit val toast = Json.format[ToastMessage]
  implicit val flip = Json.format[FlipData]
  implicit val iconic = Json.format[IconicData]
  implicit val cycle = Json.format[CycleTile]
  implicit val json = Json.format[MPNSRequest]
}
