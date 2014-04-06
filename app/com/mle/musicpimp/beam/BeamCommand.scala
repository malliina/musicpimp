package com.mle.musicpimp.beam

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class BeamCommand(track: String,
                       uri: String,
                       username: String,
                       password: String)

object BeamCommand {
  implicit val jsonFormat = Json.format[BeamCommand]
}