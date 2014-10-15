package tests

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
  test("ObservableS.from does not replay") {
    //    val obs = Observable.from(Future {
    //      Thread sleep 100
    //      5
    //    })
    //    val obs2 = obs.map(n => {
    //      println(s"Mapped: $n")
    //      n + 1
    //    }).publish
    //    obs2.connect
    //    Thread sleep 400
    //    obs2.subscribe(n => println(n))
    //    obs2.subscribe(n => println(n))
    //    Thread sleep 400
    //    var value = 0
    //    val sub = obs.subscribe(n => value = n)
    //    sub.unsubscribe()
    //    assert(value === 0)
    //    obs.flatMap(n => Observable.from(Seq(n, n, n))).subscribe(n => println(n))
  }
}
