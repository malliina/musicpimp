package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.messaging.cloud.{ADMRequest, BasicResult}
import com.malliina.push.adm.ADMClient

import scala.concurrent.Future

class ADMHandler(client: ADMClient) extends PushRequestHandler[ADMRequest, BasicResult] {
  override def push(request: ADMRequest): Future[Seq[BasicResult]] =
    client.pushAll(request.tokens, request.message)
      .map(rs => rs.map(BasicResult.fromResponse))
}
