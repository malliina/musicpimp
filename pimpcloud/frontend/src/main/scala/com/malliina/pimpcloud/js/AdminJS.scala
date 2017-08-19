package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.CloudStrings._
import org.scalajs.jquery.JQuery
import play.api.libs.json.{JsError, JsValue, Json, Reads}

import scalatags.Text.all._

case class Server(id: String, address: String)

object Server {
  implicit val json = Json.format[Server]
}

case class Phone(s: String, address: String)

object Phone {
  implicit val json = Json.format[Phone]
}

case class Track(title: String, artist: String)

object Track {
  implicit val json = Json.format[Track]
}

case class Range(description: String)

object Range {
  implicit val json = Json.format[Range]
}

case class Request(serverID: String,
                   request: String,
                   track: Track,
                   range: Range)

object Request {
  implicit val json = Json.format[Request]
}

sealed trait AdminList

object AdminList {
  implicit val reads: Reads[AdminList] = Reads { json =>
    val body = json \ Body
    (json \ Event).validate[String].flatMap {
      case RequestsKey => body.validate[Seq[Request]].map(Requests.apply)
      case PhonesKey => body.validate[Seq[Phone]].map(Phones.apply)
      case ServersKey => body.validate[Seq[Server]].map(Servers.apply)
      case other => JsError(s"Invalid $Event key: '$other'.")
    }
  }
}

case class Requests(requests: Seq[Request]) extends AdminList

case class Phones(phones: Seq[Phone]) extends AdminList

case class Servers(servers: Seq[Server]) extends AdminList

class AdminJS extends SocketJS("/admin/usage") {
  val phonesTable: JQuery = elem(PhonesTableId)
  val serversTable = elem(ServersTableId)
  val requestsTable = elem(RequestsTableId)

  override def handlePayload(payload: JsValue): Unit = {
    handleValidated[AdminList](payload) {
      case Requests(requests) => updateRequests(requests)
      case Phones(phones) => updatePhones(phones)
      case Servers(servers) => updateServers(servers)
    }
  }

  def updateRequests(requests: Seq[Request]): Unit = {
    def row(request: Request) = tr(
      td(request.serverID),
      td(request.request),
      td(request.track.title),
      td(request.track.artist),
      td(request.range.description)
    )

    clearAndSet(requestsTable, requests, row)
  }

  def updatePhones(phones: Seq[Phone]): Unit = {
    def row(phone: Phone) = tr(td(phone.s), td(phone.address))

    clearAndSet(phonesTable, phones, row)
  }

  def updateServers(servers: Seq[Server]): Unit = {
    def row(server: Server) = tr(td(server.id), td(server.address))

    clearAndSet(serversTable, servers, row)
  }

  def clearAndSet[T](table: JQuery, es: Seq[T], toRow: T => Modifier): Unit = {
    table.find("tr").remove()
    es foreach { e => table append toRow(e).toString }
  }
}
