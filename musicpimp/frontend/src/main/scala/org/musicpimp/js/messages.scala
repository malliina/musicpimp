package org.musicpimp.js

case class Command(cmd: String)

object Command {
  val subscribe = Command("subscribe")
}

case class IdCommand(cmd: String, id: String)

case class ValuedCommand[T](cmd: String, value: T)

object ValuedCommand {
  def mute(isMute: Boolean) = ValuedCommand("mute", isMute)
}

case class TrackCommand(cmd: String, track: String)

object TrackCommand {
  def play(id: String) = TrackCommand("play", id)

  def add(id: String) = TrackCommand("add", id)
}

case class ItemsCommand(cmd: String,
                        folders: Seq[String],
                        tracks: Seq[String])

object ItemsCommand {
  def playFolder(id: String) = ItemsCommand("play_items", Seq(id), Nil)

  def addFolder(id: String) = ItemsCommand("add_items", Seq(id), Nil)
}
