package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.WindowsMessage
import com.malliina.push.mpns._
import play.api.libs.json.{Json, OFormat}

case class MPNSRequest(toast: Option[ToastMessage] = None,
                       tile: Option[TileData] = None,
                       flip: Option[FlipData] = None,
                       iconic: Option[IconicData] = None,
                       cycle: Option[CycleTile] = None) {
  val message: Option[WindowsMessage] =
    toast orElse tile orElse flip orElse iconic orElse cycle
}

object MPNSRequest {
  implicit val tile: OFormat[TileData] = Json.format[TileData]
  implicit val toast: OFormat[ToastMessage] = Json.format[ToastMessage]
  implicit val flip: OFormat[FlipData] = Json.format[FlipData]
  implicit val iconic: OFormat[IconicData] = Json.format[IconicData]
  implicit val cycle: OFormat[CycleTile] = Json.format[CycleTile]
  implicit val json: OFormat[MPNSRequest] = Json.format[MPNSRequest]
}
