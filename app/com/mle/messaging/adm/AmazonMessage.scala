package com.mle.messaging.adm

import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import com.mle.play.json.JsonFormats

/**
 * @author Michael
 */
case class AmazonMessage(data: Map[String, String], expiresAfter: Duration)

object AmazonMessage {
  implicit val durationFormat = JsonFormats.durationFormat
  implicit val json = Json.format[AmazonMessage]
}