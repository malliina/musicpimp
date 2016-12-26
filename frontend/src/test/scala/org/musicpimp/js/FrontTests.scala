package org.musicpimp.js

import upickle.Js
import utest._

import scala.concurrent.duration.{Duration, DurationInt}
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

    'Json {
      val str = PimpJSON.write(GenClass("hey", 123))
      assert(str contains ":123}")
      val str2 = PimpJSON.write(GenClass("hey2", false))
      assert(str2 contains ":false}")
    }

    'ReadOneKey {
      val obj = Js.Obj("cmd" -> Js.Str("hey"))
      val maybeCmd = obj.obj get "cmd"
      assert(maybeCmd.exists(json => json.str == "hey"))
    }

    'DurationJSON {
      val dur = PimpJSON.readJs[Duration](Js.Num(123))
      assert(dur.toSeconds == 123)
      val written = PimpJSON.write(dur)
      assert(written == "123")
    }

    'Formatting {
      val formatted = Playback.toHHMMSS(123.seconds)
      assert(formatted == "02:03")
      val formatted2 = Playback.toHHMMSS(4123.seconds)
      assert(formatted2 == "01:08:43")
    }
  }
}