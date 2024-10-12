package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMResponse, MappedGCMResponse}
import io.circe.Codec
import play.api.libs.json.{Json, OFormat, OWrites}

case class PushResult(
  apns: Seq[APNSHttpResult],
  gcm: Seq[MappedGCMResponse],
  adm: Seq[BasicResult],
  mpns: Seq[BasicResult],
  wns: Seq[WNSResult]
) {
  def isEmpty: Boolean = apns.isEmpty && gcm.isEmpty && adm.isEmpty && mpns.isEmpty && wns.isEmpty
}

object PushResult {
  val empty = PushResult(Nil, Nil, Nil, Nil, Nil)
  implicit val json: Codec[PushResult] = Codec.derived[PushResult]
}

case class PushResponse(result: PushResult)

object PushResponse {
  implicit val json: Codec[PushResponse] = Codec.derived[PushResponse]
}
