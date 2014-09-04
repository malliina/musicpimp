package com.mle.messaging.adm

import com.mle.play.json.JsonFormats
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

/**
 * @author Michael
 */
case class AccessToken(access_token: String, expires_in: Duration, scope: String, token_type: String)

object AccessToken {
  implicit val durationFormat = JsonFormats.durationFormat

  implicit val json = Json.format[AccessToken]
}