package org.musicpimp.js

import com.malliina.musicpimp.models.{CloudCommand, CloudID, Connect, Disconnect}
import org.scalatest.FunSuite
import play.api.libs.json.{Json, Writes}

import scala.concurrent.duration.DurationInt
import scalatags.Text.all._

class FrontTests extends FunSuite {
  test("trivial") {
    assert(2 - 1 == 1)
  }

  test("scalatags") {
    val m = SeqFrag(Seq(p("a"), p("b")))
    assert(m.render startsWith "<p>")
  }

  test("formatting") {
    val formatted = Playback.toHHMMSS(123.seconds)
    assert(formatted == "02:03")
    val formatted2 = Playback.toHHMMSS(4123.seconds)
    assert(formatted2 == "01:08:43")
  }

  test("JSON") {
    val asString = stringify(Connect(CloudID("test")))
    assert(asString contains CloudCommand.CmdKey)
    assert(stringify(Disconnect) contains CloudCommand.CmdKey)
  }

  def stringify[C: Writes](c: C) = Json.stringify(Json.toJson(c))
}
