package tests

import com.malliina.http.FullUrl

class PimpUrlTests extends munit.FunSuite:
  test("url"):
    val raw = "http://www.google.com/p?a=b"
    val url = FullUrl.build(raw).toOption.get
    assert(url.proto == "http")
    assert(url.hostAndPort == "www.google.com")
    assert(url.uri == "/p?a=b")
    assert(url.url == raw)

  test("no path is ok"):
    assert(FullUrl.build("http://www.google.com").isRight)

  test("no proto is not ok"):
    assert(FullUrl.build("www.google.com").isLeft)
