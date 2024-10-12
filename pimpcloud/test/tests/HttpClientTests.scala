package tests

import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.ahc.{AhcWSClientConfig, StandaloneAhcWSClient}

class HttpClientTests extends munit.FunSuite with BaseSuite:
  test("can instantiate http client"):
    implicit val as = ActorSystem("test")
    val conf = AhcWSClientConfig()
    StandaloneAhcWSClient(conf)
