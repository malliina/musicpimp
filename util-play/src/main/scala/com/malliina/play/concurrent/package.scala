package com.malliina.play

import scala.concurrent.{ExecutionContext, Future}

package object concurrent {

  implicit class FutureOps2[T](f: Future[T]) {
    def checkOrElse[U >: T](check: T => Boolean, orElse: => Future[U])(
      implicit ec: ExecutionContext
    ): Future[U] = {
      f.flatMap(t => if (check(t)) Future.successful(t) else orElse)
    }
  }

}
