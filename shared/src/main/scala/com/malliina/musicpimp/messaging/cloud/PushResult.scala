package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMResponse, MappedGCMResponse}
import play.api.libs.json.Json

case class PushResult(apns: Seq[APNSResult],
                      gcm: Seq[MappedGCMResponse],
                      adm: Seq[BasicResult],
                      mpns: Seq[BasicResult],
                      wns: Seq[WNSResult])

object PushResult {
  val empty = PushResult(Nil, Nil, Nil, Nil, Nil)
  // TODO add these to mobile-push
  implicit val gcmResponseJson = Json.writes[GCMResponse]
  implicit val mappedGcmResponseJson = Json.writes[MappedGCMResponse]
  implicit val json = Json.format[PushResult]
}
