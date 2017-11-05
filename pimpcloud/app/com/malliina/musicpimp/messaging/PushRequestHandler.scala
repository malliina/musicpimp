package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached

import scala.concurrent.Future

trait PushRequestHandler[Req, Res] {
  def push(requests: Seq[Req]): Future[Seq[Res]] =
    Future.traverse(requests)(pushOne)

  def pushOne(request: Req): Future[Res]
}
