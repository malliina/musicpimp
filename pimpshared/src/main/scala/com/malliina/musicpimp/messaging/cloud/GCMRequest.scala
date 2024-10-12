package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.gcm.{GCMMessage, GCMToken}
import io.circe.Codec
import play.api.libs.json.{Json, OFormat}

case class GCMRequest(tokens: Seq[GCMToken], message: GCMMessage)
  extends PushRequest[GCMToken, GCMMessage]

object GCMRequest:
  implicit val json: Codec[GCMRequest] = Codec.derived[GCMRequest]
