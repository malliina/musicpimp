package com.malliina.pimpcloud

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.CloudStrings.{Body, PhonesKey, RequestsKey, ServersKey}
import com.malliina.pimpcloud.ListEvent.format
import com.malliina.play.ContentRange
import play.api.libs.json._

sealed trait PimpList

object PimpList {
  implicit val reader: Reads[PimpList] =
    PimpStreams.json.map[PimpList](identity) orElse
      PimpPhones.json.map(identity) orElse
      PimpServers.json.map(identity)
}

case class PimpStream(request: RequestIdentifier,
                      serverID: CloudID,
                      track: Track,
                      range: ContentRange)

object PimpStream {
  implicit val format: OFormat[PimpStream] = Json.format[PimpStream]
}

case class PimpStreams(streams: Seq[PimpStream]) extends PimpList

object PimpStreams {
  implicit val json: Format[PimpStreams] = format(RequestsKey, PimpStreams.apply)(_.streams)
}

case class PimpPhone(s: CloudID, address: String)

object PimpPhone {
  implicit val json: OFormat[PimpPhone] = Json.format[PimpPhone]
}

case class PimpPhones(phones: Seq[PimpPhone]) extends PimpList

object PimpPhones {
  implicit val json: Format[PimpPhones] = format(PhonesKey, PimpPhones.apply)(_.phones)
}

case class PimpServer(id: CloudID, address: String)

object PimpServer {
  implicit val json: OFormat[PimpServer] = Json.format[PimpServer]
}

case class PimpServers(servers: Seq[PimpServer]) extends PimpList

object PimpServers {
  implicit val json: Format[PimpServers] = format(ServersKey, PimpServers.apply)(_.servers)
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
