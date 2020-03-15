package com.malliina.rx

import akka.actor.Scheduler

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

object Sources {
  def timeoutAfter[T](
    duration: FiniteDuration,
    promise: Promise[T]
  )(implicit s: Scheduler, ec: ExecutionContext) = {
    val soonFailing = akka.pattern.after(duration, s)(
      Future.failed(new concurrent.TimeoutException(s"Timed out after $duration."))
    )
    Future.firstCompletedOf(Seq(promise.future, soonFailing))
  }
}
