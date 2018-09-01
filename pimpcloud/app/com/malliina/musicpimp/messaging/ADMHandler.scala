package com.malliina.musicpimp.messaging

import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.cloud.{ADMPayload, BasicResult}
import com.malliina.push.adm.ADMClient

import scala.concurrent.Future

class ADMHandler(client: ADMClient) extends PushRequestHandler[ADMPayload, BasicResult] {
  override def pushOne(request: ADMPayload): Future[BasicResult] =
    client.push(request.token, request.message).map(BasicResult.fromResponse)
}
