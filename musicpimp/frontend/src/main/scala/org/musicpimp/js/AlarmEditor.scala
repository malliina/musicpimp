package org.musicpimp.js

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.scheduler.WeekDay
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import io.circe.Decoder
import org.scalajs.dom.{HTMLInputElement, fetch}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class AlarmEditor extends BaseScript with AlarmStrings with ScriptHelpers:
  val Checked = "checked"
  val CheckedSelector = ":checked"
  // element IDs in HTML
  val everyDay = WeekDay.EveryDay.map(_.shortName)
  val trackIdElem = elemAs[HTMLInputElement](TrackId)
  val everyElem = elemAs[HTMLInputElement](Every)

  everyElem.onClick(_ => onEveryDayClicked())
  everyDay.map(elem) foreach { e => e.onClick(_ => updateEveryDayCheckbox()) }
  val autoOptionsFuture = AutoOptions.fromAsync(
    req => searchFuture[Track](req).map(ts => ts.map(AutoItem.from)),
    item => trackIdElem.value = item.id
  )
  val ui: JQueryUI = MyJQuery(s".$Selector")
//  val ui: JQueryUI = document.getElementsByClassName(Selector)
  ui.autocomplete(autoOptionsFuture)
  updateEveryDayCheckbox()

  def onEveryDayClicked(): Unit =
    val newValue = everyElem.checked
    setAll(everyDay, newValue)

  def updateEveryDayCheckbox(): Unit =
    val isEveryDayClicked = everyDay.forall(isChecked)
    setAll(Seq(Every), isEveryDayClicked)

  def searchFuture[T: Decoder](term: Request): Future[Seq[T]] =
    val p = Promise[Seq[T]]()
    search[T](term): ts =>
      p.success(ts)
    p.future

  private def search[T: Decoder](term: Request)(onResults: Seq[T] => Unit) =
    fetch("/search?f=json")
    PimpQuery.getJSON(
      "/search?f=json",
      term,
      response => io.circe.parser.decode[Seq[T]](response.body).foreach(onResults)
    )

  def isChecked(id: String) = elemAs[HTMLInputElement](id).checked

  def setAll(ids: Seq[String], value: Boolean): Unit =
    ids.foreach(id => elemAs[HTMLInputElement](id).checked = true)
