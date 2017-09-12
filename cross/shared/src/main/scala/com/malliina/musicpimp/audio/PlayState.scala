package com.malliina.musicpimp.audio

import play.api.libs.json.{Format, Json, Reads, Writes}

sealed abstract class PlayState(val name: String)

object PlayState {
  val closed = Closed
  val all = Seq(
    Unrealized, Realizing, Realized, Prefetching,
    Prefetched, NoMedia, Open, Started,
    Stopped, Closed, Unknown, EndOfMedia
  )

  def isPlaying(state: PlayState) = state == Open || state == Started

  def withName(name: String): PlayState =
    all.find(_.name.toLowerCase == name.toLowerCase).getOrElse(Unknown)

  implicit val json = Format[PlayState](
    Reads(_.validate[String].map(withName)),
    Writes(state => Json.toJson(state.name))
  )
}

case object Unrealized extends PlayState("Unrealized")

case object Realizing extends PlayState("Realizing")

case object Realized extends PlayState("Realized")

case object Prefetching extends PlayState("Prefetching")

case object Prefetched extends PlayState("Prefetched")

case object NoMedia extends PlayState("NoMedia")

case object Open extends PlayState("Open")

case object Started extends PlayState("Started")

case object Stopped extends PlayState("Stopped")

case object Closed extends PlayState("Closed")

case object Unknown extends PlayState("Unknown")

case object EndOfMedia extends PlayState("EndOfMedia")
