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
  val Hidden = "hidden"
  val tableContent = elem("logTableBody")

  def handlePayload(payload: String) = {
    PimpJSON.validate[Seq[JVMLogEntry]](payload).fold(
      invalid => onInvalidData(invalid),
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
    val levelCell: Modifier = entry.stackTrace
      .map(_ => a(href := "#", id := linkId)(level))
      .getOrElse(level)

    entry.stackTrace foreach { stackTrace =>
      val errorRow = tr(`class` := Hidden, id := s"$rowId")(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.toString()
    }
    val row = tr(`class` := rowClass)(
      td(`class` := "col-md-1")(entry.timeFormatted),
      td(entry.message),
      td(entry.loggerName),
      td(entry.threadName),
      td(levelCell)
    )
    tableContent prepend row.toString()
    elem(linkId).click((_: JQueryEventObject) => toggle(rowId))
  }

  def toggle(row: String) = {
    val rowElem = global.jQuery(s"#$row")
    rowElem.toggleClass(Hidden)
    false
  }
}
