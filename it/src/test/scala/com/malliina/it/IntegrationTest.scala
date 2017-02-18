package com.malliina.it

import com.malliina.musicpimp.cloud.CloudSocket
import com.malliina.musicpimp.models.{CloudID, PimpUrl}
import org.scalatest.FunSuite
import play.api.Application
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests._

class PimpcloudServerSuite extends ServerSuite(new TestComponents(_))

class IntegrationTest extends PimpcloudServerSuite {
  val cloud = app
  val cloudPort = port
  implicit val mat = components.materializer
  val pimpcloudUri = PimpUrl("ws", s"localhost:$cloudPort", CloudSocket.path)
  val pimpOptions = TestOptions.default.copy(cloudUri = pimpcloudUri)
  val musicpimp = new MusicPimpSuite(pimpOptions)
  val pimp = musicpimp.app

  test("can do it") {
    assert(statusCode("/ping", pimp) === 200)
    assert(statusCode("/health", cloud) === 200)
  }

  test("musicpimp can connect to pimpcloud") {
    val clouds = musicpimp.components.clouds
    val expectedId = CloudID("test")
    assert(statusCode("/health", cloud) === 200)
    val healthStatus = await(AhcWSClient().url(s"http://localhost:$cloudPort/health").get()).status
    assert(healthStatus === 200)
    val id = await(clouds.connect(Option(expectedId)))
    assert(id === expectedId)
    clouds.disconnect()
  }

  def statusCode(uri: String, chosenApp: Application): Int =
    request(uri, chosenApp).header.status

  def request(uri: String, chosenApp: Application): Result = {
    val result = route(chosenApp, FakeRequest(GET, uri)).get
    await(result)
  }
}
