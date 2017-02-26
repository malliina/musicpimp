package com.malliina.pimpcloud.models

import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.ws.StreamData
import play.api.libs.json._

case class PhoneRequest(cmd: String, body: JsValue)

case class PimpStreams(streams: Seq[StreamData])

object PimpStreams {
  val RequestsKey = "requests"
  implicit val json = ListEvent.format(RequestsKey, PimpStreams.apply)(_.streams)
}

case class PimpPhone(s: CloudID, address: String)

object PimpPhone {
  implicit val json = Json.format[PimpPhone]
}

case class PimpPhones(phones: Seq[PimpPhone])

object PimpPhones {
  val PhonesKey = "phones"
  implicit val json = ListEvent.format(PhonesKey, PimpPhones.apply)(_.phones)
}

case class PimpServer(id: CloudID, address: String)

object PimpServer {
  implicit val json = Json.format[PimpServer]
}

case class PimpServers(servers: Seq[PimpServer])

object PimpServers {
  val ServersKey = "servers"
  implicit val json = ListEvent.format(ServersKey, PimpServers.apply)(_.servers)
}

object ListEvent {
  def format[T: Format, U](eventValue: String, build: Seq[T] => U)(strip: U => Seq[T]) =
    Format(reader(eventValue, build), writer(eventValue, strip))

  def reader[T: Reads, U](eventValue: String, build: Seq[T] => U): Reads[U] =
    Reads[U] { json =>
      (json \ Event).validate[String].flatMap {
        case `eventValue` =>
          (json \ Body).validate[Seq[T]].map(build)
        case other =>
          JsError(s"Invalid '$Event' value of '$other', expected '$eventValue'.")
      }
    }

  def writer[T: Writes, U](eventValue: String, strip: U => Seq[T]): Writes[U] =
    Writes[U](u => Json.obj(Event -> eventValue, Body -> strip(u)))
}
