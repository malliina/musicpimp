package tests

import scalatags.Text.all.*

class ScalaTagsTests extends munit.FunSuite:
  test("varargs attributes are rendered"):
    val link = a(href := "www.musicpimp.org", id := "elem1")("Click this")
    assert(link.render.contains("""id="elem1""""))
