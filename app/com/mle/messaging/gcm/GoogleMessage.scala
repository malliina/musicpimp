package com.mle.messaging.gcm

import play.api.libs.json.Json
import scala.concurrent.duration.Duration
import concurrent.duration.DurationLong
import com.mle.play.json.JsonFormats

/**
 *
 * @author mle
 */
case class GoogleMessage(registration_ids: Seq[String], data: Map[String, String], time_to_live: Duration = 20.seconds)

object GoogleMessage {

  implicit val durationFormat = JsonFormats.durationFormat

  implicit val format = Json.format[GoogleMessage]
}
