package org.musicpimp.js

import org.scalajs.dom

import scala.scalajs.js.Any

case class IdCommand(cmd: String, id: String)

class Alarms extends BaseScript {
  withDataId(".play")(runAP)
  withDataId(".delete")(deleteAP)
  withDataId(".stop")(_ => stopPlayback())

  def deleteAP(id: String) =
    postThenReload(IdCommand("delete", id))

  def runAP(id: String) =
    postAlarms(IdCommand("start", id))

  def stopPlayback(): Boolean = {
    postAlarms(Command("stop"))
    false
  }

  def postThenReload[C: PimpJSON.Writer](json: C) =
    postAlarms(json).done { (_: Any) =>
      dom.window.location.reload(false)
      false
    }

  def postAlarms[C: PimpJSON.Writer](json: C) =
    postAjax("/alarms", json)
}
