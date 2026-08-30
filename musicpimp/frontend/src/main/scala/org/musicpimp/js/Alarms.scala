package org.musicpimp.js

import com.malliina.musicpimp.models.{Delete, Start, Stop}
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import io.circe.Encoder
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Any

class Alarms extends BaseScript with AlarmStrings:
  withDataId(PlayClass)(runAP)
  withDataId(DeleteClass)(deleteAP)
  withDataId(StopClass)(_ => stopPlayback())

  private def deleteAP(id: String) =
    postThenReload(Delete(id))

  private def runAP(id: String) =
    postAlarms(Start(id))

  private def stopPlayback(): Boolean =
    postAlarms(Stop)
    false

  private def postThenReload[C: Encoder](json: C) =
    postAlarms(json).map: (_: Any) =>
      dom.window.location.reload()
      false

  private def postAlarms[C: Encoder](json: C): Future[org.scalajs.dom.Response] =
    postAjax("/alarms", json)
