package tests

import com.malliina.play.tags.Tags
import org.scalatest.FunSuite

import scalatags.Text.all._

class ScalaTagsTests extends FunSuite {
  test("varargs attributes are rendered") {
    val link = Tags.aHref("www.musicpimp.org", id := "elem1")("Click this")
    assert(link.render contains """id="elem1"""")
  }
}
