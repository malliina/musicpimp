package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject

case class IdCommand(cmd: String, id: String)

class Alarms extends BaseScript {
  val EveryId = "every"
  val Checked = "checked"
  val CheckedSelector = ":checked"
  val everyDay = Seq("mon", "tue", "wed", "thu", "fri", "sat", "sun")

  setup()

  def setup(): Unit = {
    withDataId(".play")(runAP)
    withDataId(".delete")(deleteAP)
    withDataId(".stop")(_ => stopPlayback())
    elem(EveryId).click((_: JQueryEventObject) => onEveryDayClicked())
    everyDay.map(elem).foreach { e =>
      e.click((_: JQueryEventObject) => updateEveryDayCheckbox())
    }
  }

  def isChecked(id: String) = elem(id).is(CheckedSelector)

  def onEveryDayClicked() = {
    val newValue = elem(EveryId).is(CheckedSelector)
    setAll(everyDay, newValue)
  }

  def updateEveryDayCheckbox() = {
    val isEveryDayClicked = everyDay.forall(isChecked)
    setAll(Seq(EveryId), isEveryDayClicked)
  }

  def deleteAP(id: String) =
    postAlarms(IdCommand("delete", id))

  def runAP(id: String) =
    postAlarms(IdCommand("start", id))

  def stopPlayback(): Boolean = {
    postAlarms(Command("stop"))
    false
  }

  def setAll(ids: Seq[String], value: Boolean) =
    ids.foreach(id => elem(id).prop(Checked, value))

  def postAlarms[C: PimpJSON.Writer](json: C) =
    postAjax("/alarms", json)
}
