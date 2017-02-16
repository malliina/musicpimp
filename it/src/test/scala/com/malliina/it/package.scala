package com.malliina

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

package object it {
  def await[T](t: Future[T], timeout: Duration = 10.seconds): T = Await.result(t, timeout)
}
