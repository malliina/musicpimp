package com.malliina.musicpimp.messaging

import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.cloud.{BasicResult, MPNSPayload}
import com.malliina.push.PushException
import com.malliina.push.mpns.MPNSClient

import scala.concurrent.Future

class MPNSHandler(client: MPNSClient) extends PushRequestHandler[MPNSPayload, BasicResult] {
  def pushOne(req: MPNSPayload): Future[BasicResult] = {
    req.message.message.map { message =>
      client.push(req.token, message).map(BasicResult.fromResponse)
    }.getOrElse {
      Future.failed(new PushException(s"No message in MPNS payload for token '${req.token}'."))
    }
  }
}
