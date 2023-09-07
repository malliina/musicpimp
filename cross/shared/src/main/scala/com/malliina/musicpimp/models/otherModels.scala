package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats.{cmd, evented, singleCmd}
import play.api.libs.json.{Json, OFormat, Reads}

case class SearchStatus(status: String)

object SearchStatus {
  val Key = "search_status"
  implicit val json: OFormat[SearchStatus] = evented(Key, Json.format[SearchStatus])
}

sealed trait SearchMessage

object SearchMessage {
  implicit val reader: Reads[SearchMessage] =
    Refresh.json.map[SearchMessage](identity).orElse(Subscribe.json.map(identity))
}

case object Refresh extends SearchMessage {
  val Key = "refresh"
  implicit val json: OFormat[Refresh.type] = singleCmd(Key, Refresh)
}

case object Subscribe extends SearchMessage {
  val Key = "subscribe"
  implicit val json: OFormat[Subscribe.type] = singleCmd(Key, Subscribe)
}

case object Stop {
  val Key = "stop"
  implicit val json: OFormat[Stop.type] = singleCmd(Key, Stop)
}

case class Start(id: String)

object Start {
  val Key = "start"
  implicit val json: OFormat[Start] = cmd(Key, Json.format[Start])
}

case class Delete(id: String)

object Delete {
  val Key = "delete"
  implicit val json: OFormat[Delete] = cmd(Key, Json.format[Delete])
}
