package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings._

case class IdCommand(cmd: String, id: String)

case class ValuedCommand[T](cmd: String, value: T)

object ValuedCommand {
  def mute(isMute: Boolean) = ValuedCommand(Mute, isMute)
}

case class ItemsCommand(cmd: String,
                        folders: Seq[String],
                        tracks: Seq[String])

object ItemsCommand {
  def playFolder(id: String) = ItemsCommand(PlayItems, Seq(id), Nil)

  def addFolder(id: String) = ItemsCommand(AddItems, Seq(id), Nil)
}
