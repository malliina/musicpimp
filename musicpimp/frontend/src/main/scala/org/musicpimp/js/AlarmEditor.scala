package org.musicpimp.js

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.scheduler.WeekDay
import com.malliina.musicpimp.scheduler.web.AlarmStrings
import io.circe.Decoder
import org.scalajs.dom.{Element, Event, HTMLInputElement, document, fetch}
import scalatags.JsDom.all.{SeqFrag, cls, div, id, stringAttr, stringFrag}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.{JSON, URIUtils}

class AlarmEditor extends BaseScript with AlarmStrings with ScriptHelpers:
  val Checked = "checked"
  val CheckedSelector = ":checked"
  // element IDs in HTML
  private val everyDay = WeekDay.EveryDay.map(_.shortName)
  private val everyElem = elemAs[HTMLInputElement](Every)
  private val trackIdElem = elemAs[HTMLInputElement](TrackId)
  private val trackElem = elemAs[HTMLInputElement](TrackKey)
  private val autocompleteClass = "autocomplete-items"
  everyElem.onClick(_ => onEveryDayClicked())
  everyDay
    .map(elem)
    .foreach: e =>
      e.onClick(_ => updateEveryDayCheckbox())
  trackElem.addEventListener(
    "input",
    (e: Event) =>
      closeLists(None)
      val term = trackElem.value
      searchFuture[Track](Request(term)).map: results =>
        val divs = results.map: track =>
          val display = s"${track.artist} - ${track.title}"
          div(id := track.id.id)(display)
        val a = div(cls := autocompleteClass)(divs)
        trackElem.parentNode.appendChild(a.render)
        results.foreach: t =>
          findElem(t.id.id).foreach: item =>
            item.addEventListener(
              "click",
              e =>
                trackElem.value = s"${t.artist} - ${t.title}"
                closeLists(None)
                trackIdElem.value = t.id.id
            )
  )
  document.addEventListener("click", e => closeLists(Option(e.target.asInstanceOf[Element])))
  updateEveryDayCheckbox()

  private def closeLists(except: Option[Element]): Unit =
    val elems = document.getElementsByClassName(autocompleteClass)
    elems.foreach: e =>
      if !except.contains(e) then e.parentNode.removeChild(e)

  private def onEveryDayClicked(): Unit =
    val newValue = everyElem.checked
    setAll(everyDay, newValue)

  private def updateEveryDayCheckbox(): Unit =
    val isEveryDayClicked = everyDay.forall(isChecked)
    setAll(Seq(Every), isEveryDayClicked)

  private def searchFuture[T: Decoder](term: Request): Future[Seq[T]] =
    val p = Promise[Seq[T]]()
    search[T](term): ts =>
      p.success(ts)
    p.future

  private def search[T: Decoder](term: Request)(onResults: Seq[T] => Unit) =
    fetch(s"/search?f=json&term=${URIUtils.encodeURIComponent(term.term)}").toFuture
      .flatMap: res =>
        res.json().toFuture
      .map: json =>
        io.circe.parser.decode[Seq[T]](JSON.stringify(json)).foreach(onResults)

  private def isChecked(id: String) = elemAs[HTMLInputElement](id).checked

  private def setAll(ids: Seq[String], value: Boolean): Unit =
    ids.foreach(id => elemAs[HTMLInputElement](id).checked = value)
