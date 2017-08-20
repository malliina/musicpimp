package org.musicpimp.js

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.scheduler.WeekDay
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import play.api.libs.json.{Json, Reads}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class AlarmEditor extends BaseScript with AlarmStrings with ScriptHelpers {
  val pimpQuery = global.jQuery.asInstanceOf[PimpQuery]

  val Checked = "checked"
  val CheckedSelector = ":checked"
  // element IDs in HTML
  val everyDay = WeekDay.EveryDay.map(_.shortName)
  val trackIdElem = elem(TrackId)

  elem(Every).click((_: JQueryEventObject) => onEveryDayClicked())
  everyDay.map(elem) foreach { e =>
    e.click((_: JQueryEventObject) => updateEveryDayCheckbox())
  }
  val autoOptionsFuture = AutoOptions.fromAsync(
    req => searchFuture[Track](req).map(ts => ts.map(AutoItem.from)),
    item => trackIdElem.value(item.id)
  )
  val ui: JQueryUI = jQuery(s".$Selector")
  ui.autocomplete(autoOptionsFuture)
  updateEveryDayCheckbox()

  def onEveryDayClicked(): Unit = {
    val newValue = elem(Every).is(CheckedSelector)
    setAll(everyDay, newValue)
  }

  def updateEveryDayCheckbox(): Unit = {
    val isEveryDayClicked = everyDay.forall(isChecked)
    setAll(Seq(Every), isEveryDayClicked)
  }

  def searchFuture[T: Reads](term: Request): Future[Seq[T]] = {
    val p = Promise[Seq[T]]()
    search[T](term)(ts => p.success(ts))
    p.future
  }

  def search[T: Reads](term: Request)(onResults: Seq[T] => Unit) =
    PimpQuery.getJSON(
      "/search?f=json",
      term,
      response => {
        Json.parse(response.body).validate[Seq[T]].foreach(onResults)
      })

  def isChecked(id: String) = elem(id).is(CheckedSelector)

  def setAll(ids: Seq[String], value: Boolean): Unit =
    ids.foreach(id => elem(id).prop(Checked, value))
}
