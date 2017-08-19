package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings._
import play.api.libs.json.{Format, Json}

case class IdCommand(cmd: String, id: String)

object IdCommand {
  implicit val json = Json.format[IdCommand]
}

case class ValuedCommand[T: Format](cmd: String, value: T)

object ValuedCommand {
  implicit def json[T: Format]: Format[ValuedCommand[T]] = Json.format[ValuedCommand[T]]

  def mute(isMute: Boolean): ValuedCommand[Boolean] =
    ValuedCommand(Mute, isMute)
}

case class ItemsCommand(cmd: String,
                        folders: Seq[String],
                        tracks: Seq[String])

object ItemsCommand {
  implicit val json = Json.format[ItemsCommand]

  def playFolder(id: String) = ItemsCommand(PlayItems, Seq(id), Nil)

  def addFolder(id: String) = ItemsCommand(AddItems, Seq(id), Nil)
}
