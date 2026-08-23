package com.malliina.musicpimp.messaging

import cats.effect.Async
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.cloud.{BasicResult, MPNSPayload}
import com.malliina.push.PushException
import com.malliina.push.mpns.MPNSClient

class MPNSHandler[F[_]: Async](client: MPNSClient)
  extends PushRequestHandler[F, MPNSPayload, BasicResult]:
  val F = Async[F]
  def pushOne(req: MPNSPayload): F[BasicResult] =
    req.message.message
      .map: message =>
        F.fromFuture(F.delay(client.push(req.token, message).map(BasicResult.fromResponse)))
      .getOrElse:
        F.raiseError(new PushException(s"No message in MPNS payload for token '${req.token}'."))
