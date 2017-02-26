package com.malliina

import scala.concurrent.{ExecutionContext, Future}

package object util {

  implicit class FutureFunctions[L, R](r: Future[Either[L, R]]) {
    def or(ifLeft: => Either[L, R])(implicit ec: ExecutionContext): Future[Either[L, R]] =
      orEither(Future.successful(ifLeft))

    def orEither(orElse: => Future[Either[L, R]])(implicit ec: ExecutionContext): Future[Either[L, R]] =
      r.flatMap(_.fold(_ => orElse, r => Future.successful(Right(r))))
  }

}
