package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.messaging.cloud.{BasicResult, MPNSRequest}
import com.malliina.push.mpns.MPNSClient

import scala.concurrent.Future

class MPNSHandler(client: MPNSClient) extends PushRequestHandler[MPNSRequest, BasicResult] {
  def push(request: MPNSRequest): Future[Seq[BasicResult]] = {
    request.message
      .map(message => client.pushAll(request.tokens, message))
      .getOrElse(Future.successful(Nil))
      .map(rs => rs.map(BasicResult.fromResponse))
  }
}
