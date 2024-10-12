package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats.{cmd, evented, singleCmd}
import io.circe.{Codec, Decoder}
import io.circe.generic.semiauto.deriveCodec
import play.api.libs.json.{Json, OFormat, Reads}

case class SearchStatus(status: String)

object SearchStatus:
  val Key = "search_status"
  implicit val json: Codec[SearchStatus] = evented(Key, deriveCodec[SearchStatus])

sealed trait SearchMessage

object SearchMessage:
  implicit val reader: Decoder[SearchMessage] =
    Refresh.json.map[SearchMessage](identity).or(Subscribe.json.map(identity))

case object Refresh extends SearchMessage:
  val Key = "refresh"
  implicit val json: Codec[Refresh.type] = singleCmd(Key, Refresh)

case object Subscribe extends SearchMessage:
  val Key = "subscribe"
  implicit val json: Codec[Subscribe.type] = singleCmd(Key, Subscribe)

case object Stop:
  val Key = "stop"
  implicit val json: Codec[Stop.type] = singleCmd(Key, Stop)

case class Start(id: String)

object Start:
  val Key = "start"
  implicit val json: Codec[Start] = cmd(Key, deriveCodec[Start])

case class Delete(id: String)

object Delete:
  val Key = "delete"
  implicit val json: Codec[Delete] = cmd(Key, deriveCodec[Delete])
