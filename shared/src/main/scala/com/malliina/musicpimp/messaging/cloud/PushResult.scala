package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMResponse, MappedGCMResponse}
import play.api.libs.json.Json

case class PushResult(apns: Seq[APNSHttpResult],
                      gcm: Seq[MappedGCMResponse],
                      adm: Seq[BasicResult],
                      mpns: Seq[BasicResult],
                      wns: Seq[WNSResult]) {
  def isEmpty: Boolean = apns.isEmpty && gcm.isEmpty && adm.isEmpty && mpns.isEmpty && wns.isEmpty
}

object PushResult {
  val empty = PushResult(Nil, Nil, Nil, Nil, Nil)
  // TODO add these to mobile-push
  implicit val gcmResponseJson = Json.writes[GCMResponse]
  implicit val mappedGcmResponseJson = Json.writes[MappedGCMResponse]
  implicit val json = Json.format[PushResult]
}

case class PushResponse(result: PushResult)

object PushResponse {
  implicit val json = Json.format[PushResponse]
}
