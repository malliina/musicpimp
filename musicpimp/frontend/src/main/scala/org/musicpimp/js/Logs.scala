package org.musicpimp.js

import java.util.UUID

import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQueryEventObject
import upickle.Js

import scalatags.Text.all._

case class JVMLogEntry(level: String,
                       message: String,
                       loggerName: String,
                       threadName: String,
                       timeFormatted: String,
                       stackTrace: Option[String] = None)

class Logs extends SocketJS("/ws/logs?f=json") {
  val LogTableId = "logTableBody"
  val tableContent = elem(LogTableId)

  override def onConnected(e: Event) = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: Js.Value) =
    PimpJSON.validateJs[Seq[JVMLogEntry]](payload).fold(
      invalid => onJsonFailure(invalid),
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
