package org.musicpimp.js

import utest._

import scala.concurrent.duration.DurationInt
import scalatags.Text.all._

case class GenClass[T](cmd: String, value: T)

object FrontTests extends TestSuite {
  override def tests = TestSuite {
    'Trivial {
      assert(2 - 1 == 1)
    }

    'ScalaTags {
      val m = SeqFrag(Seq(p("a"), p("b")))
      assert(m.render startsWith "<p>")
    }

    'Formatting {
      val formatted = Playback.toHHMMSS(123.seconds)
      assert(formatted == "02:03")
      val formatted2 = Playback.toHHMMSS(4123.seconds)
      assert(formatted2 == "01:08:43")
    }
  }
}
