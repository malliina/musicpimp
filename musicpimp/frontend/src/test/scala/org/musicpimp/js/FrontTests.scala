package org.musicpimp.js

import com.malliina.musicpimp.models.{CloudCommand, CloudID, Connect, Disconnect}
import play.api.libs.json.{Json, Writes}

import scala.concurrent.duration.DurationInt
import scalatags.Text.all._

class FrontTests extends munit.FunSuite {
  test("trivial") {
    assertEquals(2 - 1, 1)
  }

  test("scalatags") {
    val m = SeqFrag(Seq(p("a"), p("b")))
    assert(m.render startsWith "<p>")
  }

  test("formatting") {
    val formatted = Playback.toHHMMSS(123.seconds)
    assertEquals(formatted, "02:03")
    val formatted2 = Playback.toHHMMSS(4123.seconds)
    assertEquals(formatted2, "01:08:43")
  }

  test("JSON") {
    val asString = stringify(Connect(CloudID("test")): CloudCommand)
    assert(asString contains CloudCommand.CmdKey)
    assert(stringify(Disconnect: CloudCommand) contains CloudCommand.CmdKey)
  }

  def stringify[C: Writes](c: C) = Json.stringify(Json.toJson(c))
}
