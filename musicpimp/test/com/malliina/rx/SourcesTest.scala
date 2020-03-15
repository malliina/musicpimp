package com.malliina.rx

import com.malliina.streams.StreamsUtil
import tests.AsyncSuite

import scala.concurrent.Promise

class SourcesTest extends AsyncSuite {
  test("Sources.connectedStream emites messages consumed by sink") {
    val hub = StreamsUtil.connectedStream[String]()
    val sent = "hi"
    hub.sink.send("hi")
    val p = Promise[String]()
    hub.source.take(1).runForeach { str =>
      p.success(str)
    }
    val received = await(p.future)
    assert(sent === received)
    hub.killSwitch.shutdown()
  }
}
