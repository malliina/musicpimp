package org.musicpimp.js

import com.malliina.musicpimp.models.{Delete, Start, Stop}
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import io.circe.Encoder
import org.scalajs.dom

import scala.scalajs.js.Any

class Alarms extends BaseScript with AlarmStrings:
  withDataId(PlayClass)(runAP)
  withDataId(DeleteClass)(deleteAP)
  withDataId(StopClass)(_ => stopPlayback())

  def deleteAP(id: String) =
    postThenReload(Delete(id))

  def runAP(id: String) =
    postAlarms(Start(id))

  def stopPlayback(): Boolean =
    postAlarms(Stop)
    false

  def postThenReload[C: Encoder](json: C) =
    postAlarms(json).done: (_: Any) =>
      dom.window.location.reload()
      false

  def postAlarms[C: Encoder](json: C): JQXHR =
    postAjax("/alarms", json)
