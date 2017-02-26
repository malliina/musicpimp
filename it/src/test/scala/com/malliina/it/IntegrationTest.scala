package com.malliina.it

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.cloud.CloudSocket
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{CloudID, FullUrl, TrackID}
import com.malliina.security.SSLUtils
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.Utils
import com.malliina.ws.HttpUtil
import org.apache.commons.codec.binary.Base64
import play.api.Application
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests._

import scala.concurrent.{Future, Promise}

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
  val adminPath = "/admin/usage"

  test("can do it") {
    assert(statusCode("/ping", pimp) === 200)
    assert(statusCode("/health", cloud) === 200)
  }

  test("musicpimp can connect to pimpcloud") {
    try {
      val expectedId = CloudID("connect-test")
      val id = await(cloudClient.connect(Option(expectedId)))
      assert(id === expectedId)
    } finally {
      cloudClient.disconnectAndForget()
    }
  }

  test("server notifications") {
    val handler = new TestHandler
    try {
      withPimpSocket(adminPath, handler.handle) { client =>
        assert(client.isConnected)
        val result = await(handler.all())
        assert(result === 42)
        val next = handler.next(msg => (msg \ "body" \\ "id").nonEmpty)
        val joinId = CloudID("join-test")
        val id = await(cloudClient.connect(Option(joinId)))
        assert(id === joinId)
        val msg = await(next)
        val cloudId = (msg \ "body" \\ "id").headOption.flatMap(_.asOpt[CloudID])
        assert(cloudId contains id)
      }
    } finally {
      cloudClient.disconnectAndForget()
    }
  }

  test("phone notifications") {
    try {
      val expectedId = CloudID("phone-test")
      val id = await(cloudClient.connect(Option(expectedId)))
      assert(id === expectedId)
      val p = Promise[JsValue]()
      def onJson(json: JsValue) = {
        for {
          _ <- (json \ "event").asOpt[String].filter(_ == "phones")
          cloudId <- (json \ "body" \\ "s").headOption.flatMap(_.asOpt[CloudID])
        } yield p trySuccess json
      }
      withPimpSocket(adminPath, onJson) { adminSocket =>
        withPhoneSocket("/ws/playback", id, _ => ()) { phoneSocket =>
          val json = await(p.future)
        }
      }
    } finally {
      cloudClient.disconnectAndForget()
    }
  }

  test("stream notifications") {
    val p = Promise[String]()
    def onJson(json: JsValue) = {
      for {
        _ <- (json \ "event").asOpt[String].filter(_ == "requests")
        firstTrack <- (json \ "body" \\ "track").headOption
        title <- (firstTrack \ "title").asOpt[String]
      } yield p success title
    }
    withCloudTrack("notification-test") { (trackId, _, cloudId) =>
      withPimpSocket(adminPath, onJson) { _ =>
        val f = req(s"http://$cloudHostPort/tracks/$trackId", cloudId).get()
        val title = await(p.future)
        assert(title === "Test of MP3 File")
        await(f)
      }
    }
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

  class TestHandler {
    val requests = Promise[JsValue]()
    val phones = Promise[JsValue]()
    val servers = Promise[JsValue]()

    var expected: Option[Promise[JsValue]] = None
    var predicate: JsValue => Boolean = json => true

    def next(pred: JsValue => Boolean = _ => true): Future[JsValue] = {
      predicate = pred
      val p = Promise[JsValue]()
      expected = Option(p)
      p.future
    }

    def handle(json: JsValue) = {
      expected.filter(_ => predicate(json)).map { e =>
        e success json
        expected = None
        true
      }.getOrElse {
        (json \ "event").validate[String].map {
          case "requests" => requests.success(json)
          case "phones" => phones.success(json)
          case "servers" => servers.success(json)
          case _ => ()
        }
      }
    }

    def all() = for {
      req <- requests.future
      pho <- phones.future
      ser <- servers.future
    } yield 42
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
    } finally {
      cloudClient.disconnectAndForget()
    }
  }

  def req(url: String, cloudId: CloudID, headers: (String, String)*) = {
    val enc = cloudAuthorization(cloudId)
    val hs = headers :+ (HeaderNames.AUTHORIZATION -> s"Basic $enc")
    httpClient.url(url).withHeaders(hs: _*)
  }

  def cloudAuthorization(cloudId: CloudID) =
    Base64.encodeBase64String(s"$cloudId:admin:test".getBytes(StandardCharsets.UTF_8))

  def statusCode(uri: String, chosenApp: Application): Int =
    request(uri, chosenApp).header.status

  def request(uri: String, chosenApp: Application): Result = {
    val result = route(chosenApp, FakeRequest(GET, uri)).get
    await(result)
  }

  def withPhoneSocket[T](path: String, cloudId: CloudID, onMessage: JsValue => Any)(code: TestSocket => T) = {
    val authValue = s"Basic ${cloudAuthorization(cloudId)}"
    withCloudSocket(path, authValue, onMessage)(code)
  }

  def withPimpSocket[T](path: String, onMessage: JsValue => Any)(code: TestSocket => T) =
    withCloudSocket(path, HttpUtil.authorizationValue("u", "p"), onMessage)(code)

  def withCloudSocket[T](path: String, authValue: String, onMessage: JsValue => Any)(code: TestSocket => T) = {
    val uri = new URI(s"ws://$cloudHostPort$path")
    Utils.using(new TestSocket(uri, authValue, onMessage)) { client =>
      await(client.initialConnection)
      code(client)
    }
  }

  class TestSocket(wsUri: URI, authValue: String,onJson: JsValue => Any) extends SocketClient(
    wsUri,
    SSLUtils.trustAllSslContext().getSocketFactory,
    Seq(HttpUtil.Authorization -> authValue)
  ) {
    override def onText(message: String) = onJson(Json.parse(message))

    def sendJson[C: Writes](message: C) = send(Json.stringify(Json.toJson(message)))
  }
}
