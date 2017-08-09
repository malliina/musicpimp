package tests

import com.malliina.musicpimp.assets.AppAssets
import org.scalatest.FunSuite

import scalatags.Text.all._

class TagsTest extends FunSuite {
  test("seq") {
    val m = SeqFrag(Seq(p("a"), p("b")))
    assert(m.render startsWith "<p>")
    val m2: Modifier = Seq(p("a"), p("b"))
    assert(!(m2.toString startsWith "<p>"))
  }

  test("assets") {
    assert(AppAssets.css.custom.toString === "/assets/css/custom.css")
  }
}
