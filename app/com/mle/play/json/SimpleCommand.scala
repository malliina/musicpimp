package com.mle.play.json

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class SimpleCommand(cmd: String)

object SimpleCommand {
  implicit val json = Json.format[SimpleCommand]
}

