package org.musicpimp.js

import com.malliina.musicpimp.models.{Delete, Start, Stop}
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import org.scalajs.dom
import play.api.libs.json.Writes

import scala.scalajs.js.Any

class Alarms extends BaseScript with AlarmStrings {
  withDataId(s".$PlayClass")(runAP)
  withDataId(s".$DeleteClass")(deleteAP)
  withDataId(s".$StopClass")(_ => stopPlayback())

  def deleteAP(id: String) =
    postThenReload(Delete(id))

  def runAP(id: String) =
    postAlarms(Start(id))

  def stopPlayback(): Boolean = {
    postAlarms(Stop)
    false
  }

  def postThenReload[C: Writes](json: C) =
    postAlarms(json).done { (_: Any) =>
      dom.window.location.reload(false)
      false
    }

  def postAlarms[C: Writes](json: C) =
    postAjax("/alarms", json)
}
