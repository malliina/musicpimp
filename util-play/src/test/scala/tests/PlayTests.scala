package tests

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}

class PlayTests extends munit.FunSuite:
  implicit val system: ActorSystem = ActorSystem("QuickStart")

  test("can run test"):
    assert(1 + 1 == 2)

  test("stream"):
    val expected = 42
    val completion = Promise[Int]()
    val source = Source(1 to 10)
    val sinkWithCleanup = Sink.onComplete[Int](_ => completion.trySuccess(expected))
    source.runWith(sinkWithCleanup)
    val answer = await(completion.future)
    assert(answer == expected)

  override def afterAll(): Unit =
    await(system.terminate())
    super.afterAll()

  def await[T](f: Future[T]) = Await.result(f, 10.seconds)
