package com.malliina.musicpimp.beam

import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.musicpimp.models.TrackID
import com.malliina.values.{Password, Username}
import play.api.libs.json.{Json, OFormat}

case class BeamCommand(track: TrackID,
                       uri: FullUrl,
                       username: Username,
                       password: Password) extends PimpMessage

object BeamCommand {
  implicit val jsonFormat: OFormat[BeamCommand] = Json.format[BeamCommand]
}
