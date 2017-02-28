package tests

import com.malliina.play.http.FullUrl
import org.scalatest.FunSuite

class PimpUrlTests extends FunSuite {
  test("url") {
    val raw = "http://www.google.com/p?a=b"
    val url = FullUrl.build(raw).get
    assert(url.proto === "http")
    assert(url.hostAndPort === "www.google.com")
    assert(url.uri === "/p?a=b")
    assert(url.url === raw)
  }

  test("no path is ok") {
    assert(FullUrl.build("http://www.google.com").isDefined)
  }

  test("no proto is not ok") {
    assert(FullUrl.build("www.google.com").isEmpty)
  }
}
