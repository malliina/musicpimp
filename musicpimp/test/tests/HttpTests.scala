package tests

import controllers.musicpimp.Rest
import org.scalatest.FunSuite

class HttpTests extends FunSuite {
  test("http client") {
    val client = Rest.sslClient
    client.close()
  }
}
