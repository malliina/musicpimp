package tests

import java.net.URL

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class LogStreamTests extends munit.FunSuite {
  test("conn".ignore) {
    new URL("https://letsencrypt.org/").openConnection.connect()
  }

  def await[T](f: Future[T]) = Await.result(f, 10.seconds)
}
