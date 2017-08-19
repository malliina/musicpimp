package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings.{Add, Play}
import com.malliina.musicpimp.json.SocketStrings.Subscribe
import play.api.libs.json._

case class TrackCommand(cmd: String, track: String)

object TrackCommand {
  implicit val json = Json.format[TrackCommand]

  def play(id: String) = apply(Play, id)

  def add(id: String) = apply(Add, id)
}

case class Command(cmd: String)

object Command {
  implicit val json = Json.format[Command]
  val subscribe = Command(Subscribe)
}

case class Volume private(volume: Int)

object Volume {
  implicit val reader = Reads[Volume] { json =>
    json.validate[Int].flatMap { v =>
      if (v >= 0 && v <= 100) JsSuccess(apply(v))
      else JsError(s"Invalid volume: '$v'.")
    }
  }
}

sealed abstract class PlayerState(val name: String)

object PlayerState {
  implicit val json = Format[PlayerState](
    Reads(json => json.validate[String].map(forString)),
    Writes(state => Json.toJson(state.name))
  )

  case object Started extends PlayerState("Started")

  case object Stopped extends PlayerState("Stopped")

  case object NoMedia extends PlayerState("NoMedia")

  case class Unknown(n: String) extends PlayerState(n)

  def forString(s: String) = s match {
    case Started.name => Started
    case Stopped.name => Stopped
    case NoMedia.name => NoMedia
    case other => Unknown(other)
  }
}
