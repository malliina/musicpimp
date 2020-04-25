package tests

import controllers.musicpimp.Rest

class HttpTests extends munit.FunSuite {
  test("http client") {
    val client = Rest.sslClient
    client.close()
  }
}
