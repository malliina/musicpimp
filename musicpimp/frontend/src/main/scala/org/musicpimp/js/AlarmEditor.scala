package org.musicpimp.js

import org.scalajs.jquery.{JQueryEventObject, JQueryXHR, jQuery}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSON

class AlarmEditor extends BaseScript {
  val pimpQuery = global.jQuery.asInstanceOf[PimpQuery]

  val EveryId = "every"
  val Checked = "checked"
  val CheckedSelector = ":checked"
  val everyDay = Seq("mon", "tue", "wed", "thu", "fri", "sat", "sun")
  val trackIdElem = elem("track_id")

  elem(EveryId).click((_: JQueryEventObject) => onEveryDayClicked())
  everyDay.map(elem) foreach { e =>
    e.click((_: JQueryEventObject) => updateEveryDayCheckbox())
  }
  val autoOptionsFuture = AutoOptions.fromAsync(
    req => searchFuture[Track](req).map(ts => ts.map(AutoItem.from)),
    item => trackIdElem.value(item.id))
  val ui: JQueryUI = jQuery(".selector")
  ui.autocomplete(autoOptionsFuture)
  updateEveryDayCheckbox()

  def onEveryDayClicked() = {
    val newValue = elem(EveryId).is(CheckedSelector)
    setAll(everyDay, newValue)
  }

  def updateEveryDayCheckbox() = {
    val isEveryDayClicked = everyDay.forall(isChecked)
    setAll(Seq(EveryId), isEveryDayClicked)
  }

  def searchFuture[T: PimpJSON.Reader](term: Request): Future[Seq[T]] = {
    val p = Promise[Seq[T]]()
    search[T](term)(ts => p.success(ts))
    p.future
  }

  def search[T: PimpJSON.Reader](term: Request)(onResults: Seq[T] => Unit) =
    PimpQuery.getJSON(
      "/search?f=json",
      term,
      response => {
        val results = PimpJSON.read[Seq[T]](response.body)
        onResults(results)
      })

  def isChecked(id: String) = elem(id).is(CheckedSelector)

  def setAll(ids: Seq[String], value: Boolean) =
    ids.foreach(id => elem(id).prop(Checked, value))
}
