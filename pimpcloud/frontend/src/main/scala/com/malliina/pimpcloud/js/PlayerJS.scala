package com.malliina.pimpcloud.js

import com.malliina.musicpimp.models.{AddTrack, PlayTrack, TrackID}
import com.malliina.pimpcloud.CloudStrings.{PlayLink, PlaylistLink}
import org.scalajs.dom.document
import org.scalajs.dom.Event

class PlayerJS extends SocketJS("/mobile/ws") {
  installHandlers(PlayLink, "play-", play)
  installHandlers(PlaylistLink, "add-", add)

  def play(id: TrackID): Unit = send(PlayTrack(id))

  def add(id: TrackID): Unit = send(AddTrack(id))

  private def installHandlers(className: String, prefix: String, onClick: TrackID => Unit) =
    elems(className) foreach { elem =>
      val trackId = TrackID(elem.attributes.getNamedItem("id").value.drop(prefix.length))
      elem.addEventListener("click", (_: Event) => onClick(trackId), useCapture = false)
    }

  def elems(className: String) = document getElementsByClassName className
}
