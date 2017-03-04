package com.malliina.pimpcloud.js

import org.scalajs.dom.document
import org.scalajs.dom.raw.Event

case class PlayerCommand(cmd: String, track: String)

object PlayerCommand {
  def play(id: String) = apply("play", id)

  def add(id: String) = apply("add", id)
}

class PlayerJS extends SocketJS("/mobile/ws") {
  installHandlers("play-link", "play-", play)
  installHandlers("playlist-link", "add-", add)

  override def handlePayload(payload: String) = ()

  def play(id: String) = send(PlayerCommand.play(id))

  def add(id: String) = send(PlayerCommand.add(id))

  def send(cmd: PlayerCommand) = socket send PimpJSON.write(cmd)

  def installHandlers(className: String, prefix: String, id: String => Unit) =
    elems(className) foreach { elem =>
      val trackId = elem.attributes.getNamedItem("id").value.drop(prefix.length)
      elem.addEventListener("click", (_: Event) => id(trackId), useCapture = false)
    }

  def elems(className: String) = document getElementsByClassName className
}
