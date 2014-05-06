package com.mle.messaging.gcm

import play.api.libs.json.{Json, JsValue, Format, JsResult}
import scala.concurrent.duration.Duration
import concurrent.duration.DurationLong

/**
 *
 * @author mle
 */
case class GoogleMessage(registration_ids: Seq[String], data: Map[String, String], time_to_live: Duration = 20.seconds)

object GoogleMessage {

  implicit object durationFormat extends Format[Duration] {
    override def writes(o: Duration): JsValue = Json.toJson(o.toSeconds)

    override def reads(json: JsValue): JsResult[Duration] = json.validate[Long].map(_.seconds)
  }

  implicit val format = Json.format[GoogleMessage]
}
