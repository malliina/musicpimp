package com.malliina.musicpimp.messaging

import cats.effect.Async
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.cloud.{WNSPayload, WNSResult}
import com.malliina.push.PushException
import com.malliina.push.wns.{WNSClient, WNSResponse}

class WNSHandler[F[_]: Async](client: WNSClient)
  extends PushRequestHandler[F, WNSPayload, WNSResult]:
  val F = Async[F]

  override def pushOne(request: WNSPayload): F[WNSResult] =
    request.message.message
      .map: message =>
        F.fromFuture(F.delay(client.push(request.token, message).map(toResult)))
      .getOrElse:
        F.raiseError(new PushException(s"No message in WNS payload for token '${request.token}'."))

  def toResult(response: WNSResponse): WNSResult =
    WNSResult(response.reason, response.description, response.statusCode, response.isSuccess)
