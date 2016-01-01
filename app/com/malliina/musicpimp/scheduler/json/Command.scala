package com.malliina.musicpimp.scheduler.json

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class Command(cmd: String, id: String)

object Command {
  implicit val jsonFormat = Json.format[Command]
}