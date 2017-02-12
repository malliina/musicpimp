package com.malliina.concurrent

import com.malliina.concurrent.ExecutionContexts.cached
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

object Observables {
  def after[T](duration: Duration)(code: => T): Future[T] = {
    val p = Promise[T]()
    lazy val codeEval = code
    val sub = observeAfter(duration).subscribe(_ => p trySuccess codeEval)
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }

  def observeAfter(duration: Duration) = Observable.interval(duration).take(1)

  def combineAll[T](obs: List[Observable[T]], f: (T, T) => T): Observable[T] = obs match {
    case Nil => Observable.never
    case h :: t => h.combineLatestWith(combineAll(t, f))(f)
  }
}
