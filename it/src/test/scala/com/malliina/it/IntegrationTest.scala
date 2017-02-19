package com.malliina.it

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.malliina.musicpimp.cloud.CloudSocket
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{CloudID, FullUrl, TrackID}
import org.apache.commons.codec.binary.Base64
import play.api.Application
import play.api.http.HeaderNames
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests._
import com.malliina.storage.{StorageLong, StorageSize}

abstract class PimpcloudServerSuite extends ServerSuite(new TestComponents(_))

class IntegrationTest extends PimpcloudServerSuite {
  val cloud = app
  val cloudPort = port
  implicit val mat = components.materializer
  val cloudHostPort = s"localhost:$cloudPort"
  val pimpcloudUri = FullUrl("ws", cloudHostPort, CloudSocket.path)
  val pimpOptions = TestOptions.default.copy(cloudUri = pimpcloudUri)
  val musicpimp = new MusicPimpSuite(pimpOptions)
  val cloudClient = musicpimp.components.clouds
  val pimp = musicpimp.app
  val httpClient = AhcWSClient()

  test("can do it") {
    assert(statusCode("/ping", pimp) === 200)
    assert(statusCode("/health", cloud) === 200)
  }

  test("musicpimp can connect to pimpcloud") {
    val expectedId = CloudID("connect-test")
    val id = await(cloudClient.connect(Option(expectedId)))
    assert(id === expectedId)
    cloudClient.disconnectAndForget()
  }

  test("serve entire track") {
    withCloudTrack("track-test") { (trackId, fileSize, cloudId) =>
      // request track
      val r = await(req(s"http://$cloudHostPort/tracks/$trackId", cloudId).get())
      assert(r.status === 200)
      assert(r.bodyAsBytes.size.toLong === fileSize.toBytes)
    }
  }

  test("serve ranged track") {
    withCloudTrack("range-test") { (trackId, _, cloudId) =>
      // request track
      // the end of the range is inclusive
      val r = await(req(s"http://$cloudHostPort/tracks/$trackId", cloudId, HeaderNames.RANGE -> s"bytes=10-20").get())
      assert(r.status === 206)
      assert(r.bodyAsBytes.size.toLong === 11)
    }
  }

  def withCloudTrack(desiredId: String)(code: (TrackID, StorageSize, CloudID) => Any) = {
    try {
      // make sure musicpimp server has a track to serve
      val trackFile = TestUtils.makeTestMp3()
      val fileSize = Files.size(trackFile).bytes
      assert(fileSize.toBytes === 198658L)
      val trackFolder = trackFile.getParent
      Library.setFolders(Seq(trackFolder))
      val trackId = TrackID(trackFile.getFileName.toString)
      val file = Library.findAbsolute(trackId)
      assert(file.isDefined)
      // connect to pimpcloud
      val cloudId = CloudID(desiredId)
      val id = await(cloudClient.connect(Option(cloudId)))
      assert(id === cloudId)
      code(trackId, fileSize, id)
    } finally  {
      cloudClient.disconnectAndForget()
    }
  }

  def req(url: String, cloudId: CloudID, headers: (String, String)*) = {
    val enc = Base64.encodeBase64String(s"$cloudId:admin:test".getBytes(StandardCharsets.UTF_8))
    val hs = headers :+ (HeaderNames.AUTHORIZATION -> s"Basic $enc")
    httpClient.url(url).withHeaders(hs: _*)
  }

  def statusCode(uri: String, chosenApp: Application): Int =
    request(uri, chosenApp).header.status

  def request(uri: String, chosenApp: Application): Result = {
    val result = route(chosenApp, FakeRequest(GET, uri)).get
    await(result)
  }
}
