package com.malliina.play.concurrent

import scala.concurrent.{ExecutionContext, Future}

object FutureUtils {

  /** Sequentially evaluates `f` on elements in `ts` until `p` evaluates to true.
    *
    * If `p` does not evaluate to true to any element in `ts`, the result of `f`
    * on the last element in `ts` is returned. If `ts` is empty a failed `Future`
    * is returned.
    *
    * @return the first result that satisfies `p`, or the last result if there's no match
    */
  def first[T, R](
    ts: List[T]
  )(f: T => Future[R])(p: R => Boolean)(implicit ec: ExecutionContext): Future[R] =
    ts match {
      case Nil =>
        Future.failed(new NoSuchElementException)
      case head :: tail =>
        f(head) flatMap { res =>
          if (p(res) || tail.isEmpty) fut(res)
          else first(tail)(f)(p)
        }
    }

  def fut[T](t: T): Future[T] = Future.successful(t)
}
