package tests

import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

trait BaseSuite {
  self: FunSuite =>
  val timeout = 5.seconds

  def await[T](f: Future[T]): T = Await.result(f, timeout)
}
