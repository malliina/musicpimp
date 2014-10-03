package tests

import org.scalatest.FunSuite
import rx.lang.scala.subjects.AsyncSubject
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
class RxTests extends FunSuite {
  test("rx hot/cold") {
    //    def hot(i: Int) = HotObservable[Int](sub => {
    //      sub.onNext(i)
    //      sub.onCompleted()
    //    })
    //    HotObservable.from(5).subscribe(n => println(n))
    //    Observable.from(Future(6)).subscribe(n => println(n))
    //    Observable.from(Seq(7)).subscribe(n => println(n))
    //    val obs = Observable.from(Seq {
    //      println(s"Item: 8")
    //      8
    //    })
    //    obs.publish.connect
    //    val futObs = HotObservable.run(Observable.from(Future(9)).doOnCompleted(println("completed"))).doOnCompleted(println("HOT done"))
    //    futObs.map(i => println(i))
    //    val ob = futObs flatMap (i => {
    //      println(i)
    //      hot(i)
    //    })
    //    val ob2 = ob.doOnCompleted(println("hmm")).doOnError(err => println(s"ERROR"))
    //    HotObservable.run(ob2)
    //    //    futObs.subscribe(n => println(n), err => println("ERROR"), () => println("COMPL"))
    //    //    obs.subscribe(n => println(n))
    //    val compl = Observable.from(Future(10))
    //    compl.subscribe(i => println(i))
    //    Thread sleep 500
    //    compl.subscribe(i => println(s"Again: $i"))
    //    Thread sleep 500
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
    val obs = Observable.from(Future {
      Thread sleep 100
      5
    })
    val obs2 = obs.map(n => {
      println(s"Mapped: $n")
      n + 1
    }).publish
    obs2.connect
    Thread sleep 400
    obs2.subscribe(n => println(n))
    obs2.subscribe(n => println(n))
    Thread sleep 400
    var value = 0
    val sub = obs.subscribe(n => value = n)
    sub.unsubscribe()
    assert(value === 0)
    obs.flatMap(n => Observable.from(Seq(n, n, n))).subscribe(n => println(n))
  }
}
