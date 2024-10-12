package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings.{HiddenClass, LogTableBodyId}
import com.malliina.musicpimp.models.{JVMLogEntry, Subscribe}
import io.circe.Json
import org.musicpimp.js.Logs.randomString
import org.scalajs.dom.{Event, document}
import scalatags.JsDom.all.*

object Logs:
  private val chars = "abcdefghijklmnopqrstuvwxyz"

  private def randomString(ofLength: Int): String =
    (0 until ofLength)
      .map: _ =>
        chars.charAt(math.floor(math.random() * chars.length).toInt)
      .mkString

class Logs extends SocketJS("/ws/logs?f=json"):
  val CellContent = "cell-content"
  val CellWide = "cell-wide"
  val LogRow = "log-row"
  val TimestampCell = "cell-timestamp"
  val tableContent = elem(LogTableBodyId)

  override def onConnected(e: Event): Unit =
    send(Subscribe)
    super.onConnected(e)

  override def handlePayload(payload: Json): Unit =
    handleValidated[Seq[JVMLogEntry]](payload): entries =>
      entries.foreach(prepend)

  private def prepend(entry: JVMLogEntry): Unit =
    val rowClass = entry.level match
      case "ERROR" => "danger"
      case "WARN"  => "warning"
      case _       => ""

    val level = entry.level
    val entryId = randomString(5)
    val rowId = s"row-$entryId"
    val linkId = s"link-$entryId"
    val msgCellId = s"msg-$entryId"
    val levelCell: Modifier = entry.stackTrace
      .map(_ => a(href := "#", id := linkId)(level))
      .getOrElse(level)

    // prepends a by default hidden stacktrace row
    entry.stackTrace.foreach: stackTrace =>
      val errorRow = tr(`class` := HiddenClass, id := rowId)(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent.prepend(errorRow.render)
    // prepends the main row
    val row = tr(`class` := s"$rowClass $LogRow")(
      cell(entry.timeFormatted),
      td(`class` := s"$CellContent $CellWide", id := msgCellId)(entry.message),
      cell(lastName(entry.loggerName)),
      cell(entry.threadName),
      cell(levelCell)
    )
    tableContent.prepend(row.render)
    // adds a toggle for stacktrace visibility
    findElem(linkId).foreach: e =>
      e.onClick(_ => toggle(rowId))
    // toggles text wrapping for long texts when clicked
    elem(msgCellId).onClick: _ =>
      elem(msgCellId).toggleClass(CellContent)
//      false
    ()

  def cell = td(`class` := CellContent)

  private def lastName(path: String): String =
    val lastDot = path.lastIndexOf('.')
    if lastDot == -1 then path
    else path.drop(lastDot + 1)

  private def toggle(row: String) =
    document.getElementById(row).toggleClass(HiddenClass)
    false
