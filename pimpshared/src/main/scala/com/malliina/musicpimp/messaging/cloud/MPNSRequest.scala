package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.WindowsMessage
import com.malliina.push.mpns.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class MPNSRequest(
  toast: Option[ToastMessage] = None,
  tile: Option[TileData] = None,
  flip: Option[FlipData] = None,
  iconic: Option[IconicData] = None,
  cycle: Option[CycleTile] = None
):
  val message: Option[WindowsMessage] =
    toast.orElse(tile).orElse(flip).orElse(iconic).orElse(cycle)

object MPNSRequest:
  implicit val tile: Codec[TileData] = deriveCodec[TileData]
  implicit val toast: Codec[ToastMessage] = deriveCodec[ToastMessage]
  implicit val flip: Codec[FlipData] = deriveCodec[FlipData]
  implicit val iconic: Codec[IconicData] = deriveCodec[IconicData]
  implicit val cycle: Codec[CycleTile] = deriveCodec[CycleTile]
  implicit val json: Codec[MPNSRequest] = deriveCodec[MPNSRequest]
