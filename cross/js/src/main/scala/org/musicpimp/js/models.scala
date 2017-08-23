package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings.{Add, Play}
import com.malliina.musicpimp.json.SocketStrings.Subscribe
import com.malliina.values.IntValidator
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
