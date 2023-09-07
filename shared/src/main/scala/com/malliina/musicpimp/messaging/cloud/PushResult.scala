package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMResponse, MappedGCMResponse}
import play.api.libs.json.{Json, OFormat, OWrites}

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
  implicit val gcmResponseJson: OWrites[GCMResponse] = Json.writes[GCMResponse]
  implicit val mappedGcmResponseJson: OWrites[MappedGCMResponse] = Json.writes[MappedGCMResponse]
  implicit val json: OFormat[PushResult] = Json.format[PushResult]
}

case class PushResponse(result: PushResult)

object PushResponse {
  implicit val json: OFormat[PushResponse] = Json.format[PushResponse]
}
