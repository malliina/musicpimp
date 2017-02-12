package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.push.apns.APNSClient

import scala.concurrent.Future

class APNSHandler(client: APNSClient) extends PushRequestHandler[APNSRequest, APNSResult] {
  override def push(request: APNSRequest): Future[Seq[APNSResult]] =
    client.pushAll(request.tokens, request.message)
      .map(_.map(an => APNSResult.fromAPNS(an)))
}
