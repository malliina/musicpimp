package com.malliina.pimpcloud.js

import org.scalajs.jquery.JQuery
import upickle.{Invalid, Js}

import scalatags.Text.all._

case class AdminEvent(event: String, body: Js.Arr)

case class Server(id: String, address: String)

case class Phone(s: String, address: String)

case class Request(serverID: String,
                   request: String,
                   track: Track,
                   range: Range)

case class Track(title: String, artist: String)

case class Range(description: String)

class AdminJS extends SocketJS("/admin/usage") {
  val phonesTable: JQuery = elem("phonesTable")
  val serversTable = elem("serversTable")
  val requestsTable = elem("requestsTable")

  override def handlePayload(payload: String) = {
    val event = validate[AdminEvent](payload)
    event.fold(
      invalid => onJsonFailure(invalid),
      e => handleEvent(e)
    )
  }

  def handleEvent(event: AdminEvent) = {
    def validateRight[T: PimpJSON.Reader](json: Js.Value) =
      PimpJSON.validateJs[T](json).right

    val body = event.body
    val result = event.event match {
      case "requests" =>
        validateRight[Seq[Request]](body).map(updateRequests)
      case "phones" =>
        validateRight[Seq[Phone]](body).map(updatePhones)
      case "servers" =>
        validateRight[Seq[Server]](body).map(updateServers)
      case other =>
        Left(Invalid.Data(PimpJSON.writeJs(other), s"Unknown event: '$other'."))
    }
    result.left foreach onJsonFailure
  }

  def updateRequests(requests: Seq[Request]) = {
    def row(request: Request) = tr(
      td(request.serverID),
      td(request.request),
      td(request.track.title),
      td(request.track.artist),
      td(request.range.description)
    )

    clearAndSet(requestsTable, requests, row)
  }

  def updatePhones(phones: Seq[Phone]) = {
    def row(phone: Phone) = tr(td(phone.s), td(phone.address))

    clearAndSet(phonesTable, phones, row)
  }

  def updateServers(servers: Seq[Server]) = {
    def row(server: Server) = tr(td(server.id), td(server.address))

    clearAndSet(serversTable, servers, row)
  }

  def clearAndSet[T](table: JQuery, es: Seq[T], toRow: T => Modifier) = {
    table.find("tr").remove()
    es foreach { e => table append toRow(e).toString }
  }
}
