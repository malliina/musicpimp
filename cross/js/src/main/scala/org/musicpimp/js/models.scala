package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings.{Add, Play}
import com.malliina.musicpimp.json.SocketStrings.Subscribe
import com.malliina.musicpimp.models.TrackID
import play.api.libs.json._

case class Command(cmd: String)

object Command {
  implicit val json = Json.format[Command]
  val subscribe = Command(Subscribe)
}
