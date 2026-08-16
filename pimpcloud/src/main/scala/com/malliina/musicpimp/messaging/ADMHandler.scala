package com.malliina.musicpimp.messaging

import cats.effect.Async
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.cloud.{ADMPayload, BasicResult}
import com.malliina.push.adm.ADMClient

class ADMHandler[F[_]: Async](client: ADMClient)
  extends PushRequestHandler[F, ADMPayload, BasicResult]:
  override def pushOne(request: ADMPayload): F[BasicResult] =
    Async[F].fromFuture(
      Async[F].delay(client.push(request.token, request.message).map(BasicResult.fromResponse))
    )
