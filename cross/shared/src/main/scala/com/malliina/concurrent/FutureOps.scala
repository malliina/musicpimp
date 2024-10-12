package com.malliina.concurrent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

implicit class FutureOps[T](fut: Future[T]):
  def recoverAll[U >: T](fix: Throwable => U)(implicit ec: ExecutionContext): Future[U] =
    fut.recover:
      case NonFatal(t) => fix(t)

  def recoverWithAll[U >: T](fix: Throwable => Future[U])(implicit
    ec: ExecutionContext
  ): Future[U] =
    fut.recoverWith:
      case NonFatal(t) => fix(t)

  def orElse[U >: T](other: => Future[U])(implicit ec: ExecutionContext) =
    recoverWithAll(_ => other)

  def exists(predicate: T => Boolean)(implicit ec: ExecutionContext): Future[Boolean] =
    fut.map(predicate).recoverAll(_ => false)

  def isDefined(implicit ec: ExecutionContext) = fut.exists(_ => true)
