package org.musicpimp.js

import com.malliina.musicpimp.models.{CloudCommand, CloudID, Connect, Disconnect}
import play.api.libs.json.{Json, Writes}
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

    'JSON {
      val asString = stringify(Connect(CloudID("test")))
      assert(asString contains CloudCommand.CmdKey)
      assert(stringify(Disconnect) contains CloudCommand.CmdKey)
    }
  }

  def stringify[C: Writes](c: C) = Json.stringify(Json.toJson(c))
}
