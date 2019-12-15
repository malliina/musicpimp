package tests

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.FunSuite
import play.api.libs.ws.ahc.{AhcWSClientConfig, StandaloneAhcWSClient}

class HttpClientTests extends FunSuite with BaseSuite {
  test("can instantiate http client") {
    implicit val as = ActorSystem("test")
    val conf = AhcWSClientConfig()
    StandaloneAhcWSClient(conf)
  }
}
