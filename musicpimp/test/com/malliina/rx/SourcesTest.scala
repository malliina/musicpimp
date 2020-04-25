package com.malliina.rx

import com.malliina.streams.StreamsUtil
import tests.AsyncSuite

import scala.concurrent.Promise

class SourcesTest extends AsyncSuite {
  test("Sources.connectedStream emits messages consumed by sink") {
    val hub = StreamsUtil.connectedStream[String]()
    val sent = "hi"
    val p = Promise[String]()
    hub.source.take(1).runForeach { str =>
      p.success(str)
    }
    hub.sink.send("hi")
    val received = await(p.future)
    assert(sent == received)
    hub.killSwitch.shutdown()
  }
}
