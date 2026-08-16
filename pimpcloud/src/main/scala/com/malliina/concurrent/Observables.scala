package com.malliina.concurrent

import com.malliina.concurrent.Execution.cached

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

object Observables {
//  def after[T](duration: Duration)(code: => T): Future[T] = {
//    val p = Promise[T]()
//    lazy val codeEval = code
//    val sub = observeAfter(duration).subscribe(_ => p trySuccess codeEval)
//    val ret = p.future
//    ret.onComplete(_ => sub.unsubscribe())
//    ret
//  }
//
//  def observeAfter(duration: Duration) = Observable.interval(duration).take(1)
}
