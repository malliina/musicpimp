package tests

import com.malliina.musicpimp.models.PimpUrl
import org.scalatest.FunSuite

class PimpUrlTests extends FunSuite {
  test("url") {
    val raw = "http://www.google.com/p?a=b"
    val url = PimpUrl.build(raw).get
    assert(url.proto === "http")
    assert(url.hostAndPort === "www.google.com")
    assert(url.uri === "/p?a=b")
    assert(url.url === raw)
  }

  test("no path is ok") {
    assert(PimpUrl.build("http://www.google.com").isDefined)
  }

  test("no proto is not ok") {
    assert(PimpUrl.build("www.google.com").isEmpty)
  }
}
