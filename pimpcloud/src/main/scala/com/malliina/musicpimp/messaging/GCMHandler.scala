package com.malliina.musicpimp.messaging

import cats.Applicative
import com.malliina.musicpimp.messaging.cloud.GCMPayload
import com.malliina.push.gcm.{GoogleClientF, MappedGCMResponse}

class GCMHandler[F[_]: Applicative](client: GoogleClientF[F])
  extends PushRequestHandler[F, GCMPayload, MappedGCMResponse]:
  override def pushOne(request: GCMPayload): F[MappedGCMResponse] =
    client.push(request.token, request.message)
