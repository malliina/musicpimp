package com.malliina.musicpimp.messaging

import scala.concurrent.Future

trait PushRequestHandler[Req, Res] {
  def push(request: Req): Future[Seq[Res]]

  def push(request: Option[Req]): Future[Seq[Res]] =
    orNil(request.map(push))

  protected def orNil[T](f: Option[Future[Seq[T]]]): Future[Seq[T]] =
    f.getOrElse(Future.successful(Nil))
}
