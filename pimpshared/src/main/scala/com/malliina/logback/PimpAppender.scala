package com.malliina.logback

import _root_.fs2.Stream
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, Concurrent, IO}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import com.malliina.logback.fs2.{DefaultFS2IOAppender, FS2AppenderComps, LoggingComps}
import com.malliina.logstreams.client.FS2Appender
import com.malliina.play.ws.Sockets
import io.circe.Json
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{GraphDSL, SourceQueueWithComplete, Sink as PekkoSink, Source as PekkoSource}
import org.apache.pekko.stream.{Graph, OverflowStrategy, QueueOfferResult, SourceShape, StreamDetachedException}
import play.api.mvc.WebSocket.MessageFlowTransformer

class PimpAppender extends DefaultFS2IOAppender[IO](FS2Appender.unsafe.comps)

object PimpAppender:
  val name = "AKKA"

  def install(): Unit =
    val appender = PimpAppender()
    appender.setContext(LogbackUtils.loggerContext)
    appender.setName(name)
    appender.setTimeFormat("yyyy-MM-dd HH:mm:ss")
    LogbackUtils.installAppender(appender)

  def comps[F[_]: Async]: Resource[F, LoggingComps[F]] =
    for
      d <- Dispatcher.parallel[F]
      comps <- Resource.eval(FS2AppenderComps.io(d))
    yield comps

  implicit val circeTransformer: MessageFlowTransformer[Json, Json] = Sockets.circeTransformer

  def fs2StreamToAkkaSource[A](
    stream: Stream[IO, A]
  ): Graph[SourceShape[A], NotUsed] =
    val source = PekkoSource.queue[A](0, OverflowStrategy.backpressure)
    // A sink that runs an FS2 publisherStream when consuming the publisher actor (= materialized value) of source
    val sink = PekkoSink.foreach[SourceQueueWithComplete[A]]: p =>
      // Fire and forget Future so it runs in the background
      publisherStream[A](p, stream).compile.drain.unsafeToFuture()
      ()

    PekkoSource
      .fromGraph(GraphDSL.createGraph(source):
        implicit builder =>
          source =>
            import org.apache.pekko.stream.scaladsl.GraphDSL.Implicits.*
            builder.materializedValue ~> sink
            SourceShape(source.out))
      .mapMaterializedValue(_ => NotUsed)

  private def publisherStream[A](
    publisher: SourceQueueWithComplete[A],
    stream: Stream[IO, A]
  ): Stream[IO, Unit] =
    def publish(a: A): IO[Option[Unit]] = Async[IO]
      .fromFuture(IO.delay(publisher.offer(a)))
      .flatMap:
        case QueueOfferResult.Enqueued       => ().some.pure[IO]
        case QueueOfferResult.Failure(cause) => IO.raiseError[Option[Unit]](cause)
        case QueueOfferResult.QueueClosed    => Option.empty[Unit].pure[IO]
        case QueueOfferResult.Dropped =>
          IO.raiseError[Option[Unit]](
            new IllegalStateException(
              "This should never happen because we use OverflowStrategy.backpressure"
            )
          )
      .recover:
        // This handles a race condition between `interruptWhen` and `publish`.
        // There's no guarantee that, when the akka sink is terminated, we will observe the
        // `interruptWhen` termination before calling publish one last time.
        // Such a call fails with StreamDetachedException
        case _: StreamDetachedException => Option.empty[Unit]

    def watchCompletion: IO[Unit] =
      IO.fromFuture(IO.delay(publisher.watchCompletion())).void

    def fail(e: Throwable): IO[Unit] = IO.delay(publisher.fail(e)) >> watchCompletion

    def complete: IO[Unit] = IO.delay(publisher.complete()) >> watchCompletion

    stream
      .interruptWhen(watchCompletion.attempt)
      .evalMap(publish)
      .unNoneTerminate
      .onFinalizeCase:
        case ExitCase.Succeeded | ExitCase.Canceled => complete
        case ExitCase.Errored(e)                    => fail(e)
