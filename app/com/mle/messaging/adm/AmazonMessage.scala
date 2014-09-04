package com.mle.messaging.adm

import com.mle.play.json.JsonFormats
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

/**
 * @author Michael
 */
case class AmazonMessage(data: Map[String, String], expiresAfter: Duration)

object AmazonMessage {
  implicit val durationFormat = JsonFormats.durationFormat
  implicit val json = Json.format[AmazonMessage]
}