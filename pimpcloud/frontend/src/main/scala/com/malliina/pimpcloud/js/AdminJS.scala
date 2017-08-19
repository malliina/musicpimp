package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.CloudStrings._
import com.malliina.pimpcloud._
import org.scalajs.jquery.JQuery
import play.api.libs.json.JsValue

import scalatags.Text.all._

class AdminJS extends SocketJS("/admin/usage") {
  val phonesTable: JQuery = elem(PhonesTableId)
  val serversTable = elem(ServersTableId)
  val requestsTable = elem(RequestsTableId)

  override def handlePayload(payload: JsValue): Unit = {
    handleValidated[PimpList](payload) {
      case PimpStreams(requests) => updateRequests(requests)
      case PimpPhones(phones) => updatePhones(phones)
      case PimpServers(servers) => updateServers(servers)
    }
  }

  def updateRequests(requests: Seq[PimpStream]): Unit = {
    def row(request: PimpStream) = tr(
      td(request.serverID.id),
      td(request.request.id),
      td(request.track.title),
      td(request.track.artist),
      td(request.range.description)
    )

    clearAndSet(requestsTable, requests, row)
  }

  def updatePhones(phones: Seq[PimpPhone]): Unit = {
    def row(phone: PimpPhone) = tr(td(phone.s.id), td(phone.address))

    clearAndSet(phonesTable, phones, row)
  }

  def updateServers(servers: Seq[PimpServer]): Unit = {
    def row(server: PimpServer) = tr(td(server.id.id), td(server.address))

    clearAndSet(serversTable, servers, row)
  }

  def clearAndSet[T](table: JQuery, es: Seq[T], toRow: T => Modifier): Unit = {
    table.find("tr").remove()
    es foreach { e => table append toRow(e).toString }
  }
}
