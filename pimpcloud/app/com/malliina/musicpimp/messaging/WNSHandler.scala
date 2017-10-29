package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.messaging.cloud.{WNSPayload, WNSResult}
import com.malliina.push.PushException
import com.malliina.push.wns.{WNSClient, WNSResponse}

import scala.concurrent.Future

class WNSHandler(client: WNSClient) extends PushRequestHandler[WNSPayload, WNSResult] {
  override def pushOne(request: WNSPayload): Future[WNSResult] =
    request.message.message.map { message =>
      client.push(request.token, message).map(toResult)
    }.getOrElse {
      Future.failed(new PushException(s"No message in WNS payload for token '${request.token}'."))
    }

  def toResult(response: WNSResponse): WNSResult =
    WNSResult(response.reason, response.description, response.statusCode, response.isSuccess)
}
