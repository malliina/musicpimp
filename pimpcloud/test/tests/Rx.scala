package tests

import org.scalatest.FunSuite
import rx.lang.scala.subjects.ReplaySubject

class Rx extends FunSuite {
  val subject = ReplaySubject[Int]()

  test("replaysubject replays after completion") {
    subject onNext 1
    subject onNext 2
    subject.onCompleted()
//    subject.subscribe(next => println(next))
//    Thread sleep 200
   assert(subject.toBlocking.toList === List(1,2))
  }
}
