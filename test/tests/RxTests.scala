package tests

import java.util.concurrent.TimeoutException

import com.mle.rx.Observables
import org.scalatest.FunSuite
import rx.lang.scala.subjects.AsyncSubject
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Promise}

/**
 * @author Michael
 */
class RxTests extends FunSuite {
  test("rx hot/cold") {
    val correctAnswer = 1
    val p = Promise[Int]()
    val cold = Observable.from(Seq(1)).map(_ => p success correctAnswer)
    intercept[concurrent.TimeoutException] {
      Await.result(p.future, 100.millis)
    }
    Observables hot cold
    val result = Await.result(p.future, 100.millis)
    assert(result === correctAnswer)
  }
  test("Subject does not replay") {
    val s = Subject[Int]()
    var aValue = 0
    s.subscribe(n => aValue = n)
    s onNext 1
    var bValue = 0
    s.subscribe(n => bValue = n)
    s.onCompleted()
    assert(aValue === 1)
    assert(bValue === 0)
  }
  test("Observable.from replays all items") {
    val subject = AsyncSubject[Int]()
    subject onNext 1
    subject.onCompleted()
    var value = 0
    val sub = subject.subscribe(n => value = n)
    sub.unsubscribe()
    assert(value === 1)
  }
  test("Installing a callback on a hot Observable returns a cold Observable") {
    val p = Promise[Int]()
    val future = p.future
    val correctAnswer = 1
    val cold = Observable.from(Seq(1))
    val hot = Observables.hot(cold)
    val coldAgain = hot.doOnTerminate {
      p trySuccess correctAnswer
    }
    intercept[TimeoutException] {
      Await.result(future, 100.millis)
    }
    val hotCallback = Observables.hot(coldAgain)
    val answer = Await.result(future, 100.millis)
    assert(answer === correctAnswer)
  }
}
