package com.malliina.pimpcloud.js

import java.util.UUID

import org.scalajs.jquery.JQueryEventObject

import scalatags.Text.all._

case class JVMLogEntry(level: String,
                       message: String,
                       loggerName: String,
                       threadName: String,
                       timeFormatted: String,
                       stackTrace: Option[String] = None)

case class Command(cmd: String)

object Command {
  val Subscribe = apply("subscribe")
}

class LogsJS extends SocketJS("/admin/ws?f=json") {
  val CellContent = "cell-content"
  val CellWide = "cell-wide"
  val tableContent = elem("logTableBody")

  def handlePayload(payload: String) = {
    PimpJSON.validate[Seq[JVMLogEntry]](payload).fold(
      invalid => onJsonFailure(invalid),
      entries => entries foreach prepend
    )
  }

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

    entry.stackTrace foreach { stackTrace =>
      val errorRow = tr(`class` := Hidden, id := s"$rowId")(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.render
    }
    val row = tr(`class` := rowClass)(
      cell(entry.timeFormatted),
      td(`class` := s"$CellContent $CellWide", id := msgCellId)(entry.message),
      cell(lastName(entry.loggerName)),
      cell(entry.threadName),
      cell(levelCell)
    )
    tableContent prepend row.render
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
    rowElem.toggleClass(Hidden)
    false
  }
}
