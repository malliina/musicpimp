package com.malliina.pimpcloud.models

import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.CloudStrings.{PhonesKey, RequestsKey, ServersKey}
import com.malliina.pimpcloud.json.JsonStrings.Body
import com.malliina.pimpcloud.ws.StreamData
import com.malliina.play.models.Username
import play.api.libs.json._

case class PhoneRequest[W: Writes](cmd: String, user: Username, body: W)

case class PimpStreams(streams: Seq[StreamData])

object PimpStreams {
  implicit val json = ListEvent.format(RequestsKey, PimpStreams.apply)(_.streams)
}

case class PimpPhone(s: CloudID, address: String)

object PimpPhone {
  implicit val json = Json.format[PimpPhone]
}

case class PimpPhones(phones: Seq[PimpPhone])

object PimpPhones {
  implicit val json = ListEvent.format(PhonesKey, PimpPhones.apply)(_.phones)
}

case class PimpServer(id: CloudID, address: String)

object PimpServer {
  implicit val json = Json.format[PimpServer]
}

case class PimpServers(servers: Seq[PimpServer])

object PimpServers {
  implicit val json = ListEvent.format(ServersKey, PimpServers.apply)(_.servers)
}

object ListEvent {
  def format[T: Format, U](eventValue: String, build: Seq[T] => U)(strip: U => Seq[T]) =
    Format(reader(eventValue, build), writer(eventValue, strip))

  def reader[T: Reads, U](eventValue: String, build: Seq[T] => U): Reads[U] =
    Reads[U] { json =>
      (json \ EventKey).validate[String].flatMap {
        case `eventValue` =>
          (json \ Body).validate[Seq[T]].map(build)
        case other =>
          JsError(s"Invalid '$EventKey' value of '$other', expected '$eventValue'.")
      }
    }

  def writer[T: Writes, U](eventValue: String, strip: U => Seq[T]): Writes[U] =
    Writes[U](u => Json.obj(EventKey -> eventValue, Body -> Json.toJson(strip(u))))
}
