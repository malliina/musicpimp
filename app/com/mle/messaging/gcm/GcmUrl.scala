package com.mle.messaging.gcm

import play.api.libs.json.Json

/**
 * @param id the GCM registration ID
 * @param tag custom app-provided tag
 */
case class GcmUrl(id: String, tag: String)

object GcmUrl {
  implicit val json = Json.format[GcmUrl]
}