package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.cloud.*
import com.malliina.push.apns.{APNSMessage, APSPayload}
import io.circe.syntax.EncoderOps

class APNSBuilder:
  val Message = "Open to stop"

  def buildRequest(dest: APNSDevice): APNSPayload =
    val payload = APSPayload(Some(Left(Message)))
    val extra = Map(Cmd -> Stop.asJson, Tag -> dest.tag.asJson)
    val message = APNSMessage(payload, extra)
    APNSPayload(dest.id, message)
