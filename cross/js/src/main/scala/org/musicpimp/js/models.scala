package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings.{Add, Play}
import com.malliina.musicpimp.json.SocketStrings.Subscribe
import upickle.Js

case class TrackCommand(cmd: String, track: String)

object TrackCommand {
  def play(id: String) = apply(Play, id)

  def add(id: String) = apply(Add, id)
}

case class Command(cmd: String)

object Command {
  val subscribe = Command(Subscribe)
}

case class Volume private(volume: Int)

object Volume {
  def forInt(v: Int): Option[Volume] =
    if (v >= 0 && v <= 100) Option(Volume(v))
    else None

  def unapply(json: Js.Value): Option[Volume] = json match {
    case Js.Num(n) => forInt(n.toInt)
    case _ => None
  }
}

sealed trait PlayerState

object PlayerState {

  case object Started extends PlayerState

  case object Stopped extends PlayerState

  case object NoMedia extends PlayerState

  case class Unknown(name: String) extends PlayerState

  def forString(s: String) = s match {
    case "Started" => Started
    case "Stopped" => Stopped
    case "NoMedia" => NoMedia
    case other => Unknown(other)
  }
}
