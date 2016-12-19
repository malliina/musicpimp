package org.musicpimp.js

import org.scalajs.jquery.{JQueryAjaxSettings, JQueryEventObject, jQuery}

class Library extends BaseScript {
  setup()

  def setup() = {
    installHandler(".track.play", TrackCommand.play)
    installHandler(".track.add", TrackCommand.add)
    installHandler(".folder.play", ItemsCommand.playFolder)
    installHandler(".folder.add", ItemsCommand.addFolder)
  }

  def installHandler[C: PimpJSON.Writer](clazzSelector: String, toMessage: String => C) = {
    installClick(clazzSelector) { e =>
      Option(e.delegateTarget.getAttribute("data-id")) foreach { id =>
        postPlayback(toMessage(id))
      }
    }
  }

  def installClick(clazzSelector: String)(f: JQueryEventObject => Any) =
    jQuery(clazzSelector).click(f)

  def postPlayback[C: PimpJSON.Writer](json: C) =
    postAjax("/playback", json)

  def postAjax[C: PimpJSON.Writer](resource: String, payload: C) =
    jQuery.ajax(literal(
      url = resource,
      `type` = "POST",
      contentType = "application/json",
      data = write(payload)
    ).asInstanceOf[JQueryAjaxSettings])
}
