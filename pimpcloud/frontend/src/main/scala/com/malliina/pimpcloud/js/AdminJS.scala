package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.*
import com.malliina.pimpcloud.CloudStrings.*
import io.circe.Json
import org.scalajs.dom.{HTMLTableElement, html}
import scalatags.JsDom
import scalatags.JsDom.all.*

class AdminJS extends SocketJS("/admin/usage"):
  val phonesTable = elemAs[HTMLTableElement](PhonesTableId)
  val serversTable = elemAs[HTMLTableElement](ServersTableId)
  val requestsTable = elemAs[HTMLTableElement](RequestsTableId)

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

    clearAndSet(requestsTable, requests, row)

  private def updatePhones(phones: Seq[PimpPhone]): Unit =
    def row(phone: PimpPhone) = tr(td(phone.s.id), td(phone.address))

    clearAndSet(phonesTable, phones, row)

  private def updateServers(servers: Seq[PimpServer]): Unit =
    def row(server: PimpServer) = tr(td(server.id.id), td(server.address))

    clearAndSet(serversTable, servers, row)

  private def clearAndSet[T](
    table: HTMLTableElement,
    es: Seq[T],
    toRow: T => JsDom.TypedTag[html.TableRow]
  ): Unit =
    val rows = table.rows.size
    (1 to rows).foreach: _ =>
      table.deleteRow(0)
    es.foreach: e =>
      table.append(toRow(e).render)
