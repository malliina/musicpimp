package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.*
import com.malliina.pimpcloud.CloudStrings.*
import io.circe.Json
import org.scalajs.dom.{HTMLTableElement, html}
import scalatags.JsDom
import scalatags.JsDom.all.*

class AdminJS extends SocketJS("/admin/usage"):

  private val phonesTable = elemAs[HTMLTableElement](PhonesTableElemId)
  private val serversTable = elemAs[HTMLTableElement](ServersTableElemId)
  private val requestsTable = elemAs[HTMLTableElement](RequestsTableElemId)

  private val phonesBody = elemAs[HTMLTableElement](PhonesTableId)
  private val serversBody = elemAs[HTMLTableElement](ServersTableId)
  private val requestsBody = elemAs[HTMLTableElement](RequestsTableId)

  Seq(phonesBody, serversBody, requestsBody).foreach(_.hide())

  override def handlePayload(payload: Json): Unit =
    handleValidated[PimpList](payload):
      case PimpStreams(requests) => updateRequests(requests)
      case PimpPhones(phones)    => updatePhones(phones)
      case PimpServers(servers)  => updateServers(servers)

  private def updateRequests(requests: Seq[PimpStream]): Unit =
    def row(request: PimpStream) = tr(
      td(request.serverID.id),
      td(request.request.id),
      td(request.track.title),
      td(request.track.artist),
      td(request.range.description)
    )

    clearAndSet(requestsTable, requestsBody, requests, row)

  private def updatePhones(phones: Seq[PimpPhone]): Unit =
    def row(phone: PimpPhone) = tr(td(phone.s.id), td(phone.address))

    clearAndSet(phonesTable, phonesBody, phones, row)

  private def updateServers(servers: Seq[PimpServer]): Unit =
    def row(server: PimpServer) = tr(td(server.id.id), td(server.address))

    clearAndSet(serversTable, serversBody, servers, row)

  private def clearAndSet[T](
    table: HTMLTableElement,
    tableBody: HTMLTableElement,
    es: Seq[T],
    toRow: T => JsDom.TypedTag[html.TableRow]
  ): Unit =
    val rows = tableBody.rows.size
    (1 to rows).foreach: _ =>
      tableBody.deleteRow(0)
    es.foreach: e =>
      tableBody.append(toRow(e).render)
//    if es.isEmpty then table.hide()
//    else table.show()
