package org.musicpimp.js

import java.util.UUID

import com.malliina.musicpimp.js.FrontStrings
import com.malliina.musicpimp.models.JVMLogEntry
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQueryEventObject
import play.api.libs.json.{JsError, JsValue}

import scalatags.Text.all._

class Logs extends SocketJS("/ws/logs?f=json") with FrontStrings {
  val CellContent = "cell-content"
  val CellWide = "cell-wide"
  val LogRow = "log-row"
  val TimestampCell = "cell-timestamp"
  val tableContent = elem(LogTableBodyId)

  override def onConnected(e: Event): Unit = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: JsValue): Unit =
    payload.validate[Seq[JVMLogEntry]].fold(
      invalid => onJsonFailure(JsError(invalid)),
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
    val msgCellId = s"msg-$entryId"
    val levelCell: Modifier = entry.stackTrace
      .map(_ => a(href := "#", id := linkId)(level))
      .getOrElse(level)

    // prepends a by default hidden stacktrace row
    entry.stackTrace foreach { stackTrace =>
      val errorRow = tr(`class` := HiddenClass, id := rowId)(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.render
    }
    // prepends the main row
    val row = tr(`class` := s"$rowClass $LogRow")(
      cell(entry.timeFormatted),
      td(`class` := s"$CellContent $CellWide", id := msgCellId)(entry.message),
      cell(lastName(entry.loggerName)),
      cell(entry.threadName),
      cell(levelCell)
    )
    tableContent prepend row.render
    // adds a toggle for stacktrace visibility
    elem(linkId).click((_: JQueryEventObject) => toggle(rowId))
    // toggles text wrapping for long texts when clicked
    elem(msgCellId) click { (_: JQueryEventObject) =>
      elem(msgCellId) toggleClass CellContent
      false
    }
  }

  def cell = td(`class` := CellContent)

  def lastName(path: String) = {
    val lastDot = path lastIndexOf '.'
    if (lastDot == -1) path
    else path drop (lastDot + 1)
  }

  def toggle(row: String) = {
    val rowElem = global.jQuery(s"#$row")
    rowElem.toggleClass(HiddenClass)
    false
  }
}
