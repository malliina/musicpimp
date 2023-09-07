package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings._
import play.api.libs.json.{Format, Json, OFormat}

case class IdCommand(cmd: String, id: String)

object IdCommand {
  implicit val json: OFormat[IdCommand] = Json.format[IdCommand]
}

case class ValuedCommand[T: Format](cmd: String, value: T)

object ValuedCommand {
  implicit def json[T: Format]: Format[ValuedCommand[T]] =
    Json.format[ValuedCommand[T]]

  def mute(isMute: Boolean): ValuedCommand[Boolean] =
    ValuedCommand(Mute, isMute)
}
