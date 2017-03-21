package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.CloudStrings.{PlayLink, PlaylistLink}
import org.musicpimp.js.{PimpJSON, TrackCommand}
import org.scalajs.dom.document
import org.scalajs.dom.raw.Event

class PlayerJS extends SocketJS("/mobile/ws") {
  installHandlers(PlayLink, "play-", play)
  installHandlers(PlaylistLink, "add-", add)

  override def handlePayload(payload: String) = ()

  def play(id: String) = send(TrackCommand.play(id))

  def add(id: String) = send(TrackCommand.add(id))

  def send(cmd: TrackCommand) = socket send PimpJSON.write(cmd)

  def installHandlers(className: String, prefix: String, onClick: String => Unit) =
    elems(className) foreach { elem =>
      val trackId = elem.attributes.getNamedItem("id").value.drop(prefix.length)
      elem.addEventListener("click", (_: Event) => onClick(trackId), useCapture = false)
    }

  def elems(className: String) = document getElementsByClassName className
}
