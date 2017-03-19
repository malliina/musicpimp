package org.musicpimp.js

import com.malliina.musicpimp.scheduler.web.AlarmStrings
import org.scalajs.dom

import scala.scalajs.js.Any

class Alarms extends BaseScript with AlarmStrings {
  withDataId(".play")(runAP)
  withDataId(".delete")(deleteAP)
  withDataId(".stop")(_ => stopPlayback())

  def deleteAP(id: String) =
    postThenReload(IdCommand(Delete, id))

  def runAP(id: String) =
    postAlarms(IdCommand(Start, id))

  def stopPlayback(): Boolean = {
    postAlarms(Command(Stop))
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
