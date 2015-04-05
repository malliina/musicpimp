package tests

import com.mle.rx.Observables
import org.scalatest.FunSuite
import rx.lang.scala.subjects.AsyncSubject
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.DurationInt
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
  test("hot cold") {
    val correctAnswer = 42
    val p = Promise[Int]()
    val p2 = Promise[Int]()
    val f = p.future
    val f2 = p2.future
    val observable = Observable[Int](observer => {
      p success correctAnswer
      observer.onCompleted()
    })
    intercept[concurrent.TimeoutException] {
      Await.result(f, 100.millis)
    }
    val connectable = observable.publish
    connectable.doOnCompleted {
      // doOnCompleted returns a new cold Observable: this will never run
      p2 success 42
    }
    connectable.connect
    val answer = Await.result(f, 100.millis)
    assert(answer === correctAnswer)
    intercept[concurrent.TimeoutException] {
      Await.result(f2, 100.millis)
    }
  }
}
