package com.malliina.musicpimp.beam

import play.api.libs.json.Json

case class BeamCommand(track: String,
                       uri: String,
                       username: String,
                       password: String)

object BeamCommand {
  implicit val jsonFormat = Json.format[BeamCommand]
}
