package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.messaging.cloud.GCMRequest
import com.malliina.push.gcm.{GCMClient, MappedGCMResponse}

import scala.concurrent.Future

class GCMHandler(client: GCMClient) extends PushRequestHandler[GCMRequest, MappedGCMResponse] {
  override def push(request: GCMRequest): Future[Seq[MappedGCMResponse]] =
    client.pushAll(request.tokens, request.message)
}
