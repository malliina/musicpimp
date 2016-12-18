package org.musicpimp.js

import java.util.UUID

import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event}
import org.scalajs.jquery.JQueryEventObject

import scalatags.Text.all._

case class JVMLogEntry(level: String,
                       message: String,
                       loggerName: String,
                       threadName: String,
                       timeFormatted: String,
                       stackTrace: Option[String] = None)

class Logs extends SocketJS("/ws/logs") {
  val Hidden = "hide"
  val tableContent = elem("logTableBody")
  val okStatus = elem("okstatus")
  val failStatus = elem("failstatus")

  override def onConnected(e: Event) = {
    super.onConnected(e)
    showConnected()
  }

  override def onClosed(e: CloseEvent) = showDisconnected()

  override def onError(e: ErrorEvent) = showDisconnected()

  def showConnected() = {
    okStatus.removeClass(Hidden)
    failStatus.addClass(Hidden)
  }

  def showDisconnected() = {
    okStatus.addClass(Hidden)
    failStatus.removeClass(Hidden)
  }

  override def handlePayload(payload: String) =
    PimpJSON.validate[Seq[JVMLogEntry]](payload).fold(
      invalid => onInvalidData(invalid),
      entries => entries foreach prepend
    )

  def prepend(entry: JVMLogEntry) = {
    val rowClass = entry.level match {
      case "ERROR" => "danger"
      case "WARN" => "warning"
      case _ => ""
    }

    val level = entry.level
    val entryId = UUID.randomUUID().toString take 5
    val rowId = s"row-$entryId"
    val linkId = s"link-$entryId"
    val levelCell: Modifier = entry.stackTrace
      .map(_ => a(href := "#", id := linkId)(level))
      .getOrElse(level)

    // prepends a by default hidden stacktrace row
    entry.stackTrace foreach { stackTrace =>
      val errorRow = tr(`class` := Hidden, id := s"$rowId")(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.toString()
    }
    // prepends the main row
    val row = tr(`class` := rowClass)(
      td(`class` := "col-md-1")(entry.timeFormatted),
      td(entry.message),
      td(entry.loggerName),
      td(entry.threadName),
      td(levelCell)
    )
    tableContent prepend row.toString()
    // adds a toggle for stacktrace visibility
    elem(linkId).click((_: JQueryEventObject) => toggle(rowId))
  }

  def toggle(row: String) = {
    val rowElem = global.jQuery(s"#$row")
    rowElem.toggleClass(Hidden)
    false
  }
}
