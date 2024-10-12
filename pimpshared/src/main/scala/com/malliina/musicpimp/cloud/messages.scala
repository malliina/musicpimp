package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.values.{Password, Username}
import io.circe.Codec

trait PimpMessage

case object PingMessage extends PimpMessage
case object PongMessage extends PimpMessage
case object PingAuth extends PimpMessage
case object RootFolder extends PimpMessage

case class GetFolder(id: FolderID) extends PimpMessage derives Codec.AsObject

case class GetTrack(id: TrackID) extends PimpMessage derives Codec.AsObject

case class Search(term: String, limit: Int) extends PimpMessage derives Codec.AsObject

case object GetAlarms extends PimpMessage

case class Authenticate(username: Username, password: Password) extends PimpMessage
  derives Codec.AsObject

case class GetMeta(id: TrackID) extends PimpMessage derives Codec.AsObject
