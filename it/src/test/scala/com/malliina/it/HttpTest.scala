package com.malliina.it

import play.api.libs.ws.ahc.AhcWSClient

class HttpTest extends PimpcloudServerSuite {
  test("hmm") {
    implicit val mat = components.materializer
    val healthStatus = await(AhcWSClient().url(s"http://localhost:$port/health").get()).status
    assert(healthStatus === 200)
  }
}
