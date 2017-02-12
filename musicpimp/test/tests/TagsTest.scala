package tests

import org.scalatest.FunSuite

import scalatags.Text.all._

class TagsTest extends FunSuite {
  test("seq") {
    val m = SeqFrag(Seq(p("a"), p("b")))
    assert(m.render startsWith "<p>")
    val m2: Modifier = Seq(p("a"), p("b"))
    assert(!(m2.toString startsWith "<p>"))
  }
}
