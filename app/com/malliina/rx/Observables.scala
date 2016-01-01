package com.malliina.rx

import rx.lang.scala.{Subscription, Observable}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * @author Michael
 */
object Observables {

  implicit final class ObservableOps[T](val obs: Observable[T]) {
    /**
     * Enables Observable.hot notation.
     *
     * @return a hot [[Observable]]
     */
    def hot = Observables.hot(obs)
  }

  /**
   * Builds a new, hot [[Observable]] from a cold one. The returned observable emits items whether there are
   * subscribers or not, and all subscribers share the same stream.
   *
   * TODO This may not even work.
   *
   * @param cold cold observable
   * @tparam T item type
   * @return a new, hot observable
   */
  def hot[T](cold: Observable[T]): Observable[T] = {
    val connectable = cold.publish
//    val subscription = connectable.connect
    connectable.connect
    connectable
  }

  def fromFuture[T](body: => T)(implicit executor: ExecutionContext): Observable[T] = Observable.from(Future(body))

  def after[T](duration: Duration)(code: => T): Future[T] = {
    val p = Promise[T]()
    lazy val codeEval = code
    val sub = observeAfter(duration).subscribe(_ => p trySuccess codeEval)
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())(ExecutionContext.Implicits.global)
    ret
  }

  /**
   * Emits 0 and completes after `duration`.
   *
   * @param duration
   * @return a one-item [[Observable]]
   */
  def observeAfter(duration: Duration) = Observable.interval(duration).take(1)

  def timeoutAfter[T](duration: Duration, promise: Promise[T]) =
    after(duration)(promise tryFailure new concurrent.TimeoutException(s"Timed out after $duration."))

  def every(duration: Duration)(code: => Any): Subscription = {
    Observable.interval(duration).subscribe(_ => code)
  }
}
