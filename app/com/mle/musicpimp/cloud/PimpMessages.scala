package com.mle.musicpimp.cloud

import play.api.libs.json.{JsValue, Json}

/**
 * @author Michael
 */
object PimpMessages {

  trait PimpMessage

  trait RequestMessage extends PimpMessage {
    def request: String
  }

  case object Ping extends PimpMessage

  case object PingAuth extends PimpMessage

  case object RootFolder extends PimpMessage

  case class Folder(id: String) extends PimpMessage

  case class Track(id: String) extends PimpMessage

  case class Search(term: String, limit: Int) extends PimpMessage

  case object GetAlarms extends PimpMessage

  case class AlarmEdit(payload: JsValue) extends PimpMessage

  case class AlarmAdd(payload: JsValue) extends PimpMessage

  case class PlaybackMessage(payload: JsValue) extends PimpMessage

  case class Authenticate(username: String, password: String) extends PimpMessage

  case object GetVersion extends PimpMessage

  case object GetStatus extends PimpMessage

  implicit val searchFormat = Json.format[Search]
  implicit val folderFormat = Json.format[Folder]
  implicit val trackFormat = Json.format[Track]
  implicit val authFormat = Json.format[Authenticate]
}
