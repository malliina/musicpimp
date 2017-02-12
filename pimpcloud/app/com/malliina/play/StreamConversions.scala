package com.malliina.play

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, SourceQueue}
import play.api.Logger
import rx.lang.scala.{Observable, Observer}

import scala.concurrent.ExecutionContext

object StreamConversions {
  def observableToSource[T](obs: Observable[T], mat: Materializer): Source[T, NotUsed] = {
    val (queue, source) = Streaming.sourceQueue[T](mat)
    val _ = obs.subscribe(new QueueObserver[T](queue, mat.executionContext))
    source
  }
}

class QueueObserver[T](queue: SourceQueue[Option[T]], ec: ExecutionContext) extends Observer[T] {

  import QueueObserver.log

  override def onNext(elem: T): Unit = loggedOffer(Option(elem))

  override def onCompleted(): Unit = loggedOffer(None)

  override def onError(e: Throwable): Unit = loggedOffer(None) // ???

  def loggedOffer(elem: Option[T]) = queue.offer(elem).recover(onOfferError())(ec)

  def onOfferError(): PartialFunction[Throwable, Unit] = {
    case t: Throwable =>
      log.error("Unable to offer element", t)
  }
}

object QueueObserver {
  private val log = Logger(getClass)
}
