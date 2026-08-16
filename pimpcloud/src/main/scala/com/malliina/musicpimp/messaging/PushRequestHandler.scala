package com.malliina.musicpimp.messaging

import cats.Applicative
import cats.implicits.toTraverseOps

trait PushRequestHandler[F[_]: Applicative, Req, Res]:
  def push(requests: Seq[Req]): F[Seq[Res]] =
    requests.traverse(pushOne)

  def pushOne(request: Req): F[Res]
