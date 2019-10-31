package com.malliina.rx

import akka.NotUsed
import akka.actor.{ActorRef, Scheduler}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

object Sources {
  def onlyOnce[T, U](once: Source[T, U])(implicit mat: Materializer): Source[T, NotUsed] =
    Source.fromPublisher(once.runWith(Sink.asPublisher(fanout = true)))

  def connected[U]()(implicit mat: Materializer): (ActorRef, Source[U, NotUsed]) = {
    val publisherSink = Sink.asPublisher[U](fanout = true)
    val (processedActor, publisher) =
      Source.actorRef[U](65536, OverflowStrategy.dropHead).toMat(publisherSink)(Keep.both).run()
    (processedActor, Source.fromPublisher(publisher))
  }

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
