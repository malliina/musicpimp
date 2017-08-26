package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats.{cmd, evented, singleCmd}
import play.api.libs.json.{Json, Reads}

case class SearchStatus(status: String)

object SearchStatus {
  val Key = "search_status"
  implicit val json = evented(Key, Json.format[SearchStatus])
}

sealed trait SearchMessage

object SearchMessage {
  implicit val reader: Reads[SearchMessage] =
    Refresh.json.map[SearchMessage](identity).orElse(Subscribe.json.map(identity))
}

case object Refresh extends SearchMessage {
  val Key = "refresh"
  implicit val json = singleCmd(Key, Refresh)
}

case object Subscribe extends SearchMessage {
  val Key = "subscribe"
  implicit val json = singleCmd(Key, Subscribe)
}

case object Stop {
  val Key = "stop"
  implicit val json = singleCmd(Key, Stop)
}

case class Start(id: String)

object Start {
  val Key = "start"
  implicit val json = cmd(Key, Json.format[Start])
}

case class Delete(id: String)

object Delete {
  val Key = "delete"
  implicit val json = cmd(Key, Json.format[Delete])
}
