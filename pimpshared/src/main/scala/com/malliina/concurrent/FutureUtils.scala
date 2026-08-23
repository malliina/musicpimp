package com.malliina.concurrent

import cats.effect.Sync
import cats.implicits.toFlatMapOps

object FutureUtils:

  /** Sequentially evaluates `f` on elements in `ts` until `p` evaluates to true.
    *
    * If `p` does not evaluate to true to any element in `ts`, the result of `f` on the last element
    * in `ts` is returned. If `ts` is empty a failed `Future` is returned.
    *
    * @return
    *   the first result that satisfies `p`, or the last result if there's no match
    */
  def firstIO[F[_]: Sync, T, R](ts: List[T])(f: T => F[R])(p: R => Boolean): F[R] =
    ts match
      case Nil =>
        Sync[F].raiseError(new NoSuchElementException)
      case head :: tail =>
        f(head).flatMap: res =>
          if p(res) || tail.isEmpty then Sync[F].pure(res)
          else firstIO(tail)(f)(p)
