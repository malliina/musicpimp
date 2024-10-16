package com.malliina.it

import org.apache.pekko.stream.Materializer

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.apache.pekko.stream.scaladsl.Sink
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.cloud.CloudSocket
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{CloudID, TrackID}
import com.malliina.pimpcloud.{PimpPhone, PimpPhones, PimpServer, PimpServers, PimpStreams}
import com.malliina.http.FullUrl
import com.malliina.musicpimp.app.InitOptions
import com.malliina.security.SSLUtils
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.Util
import com.malliina.values.UnixPath
import com.malliina.ws.HttpUtil
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import munit.FunSuite
import org.apache.commons.codec.binary.Base64
import play.api.{Application, BuiltInComponents}
import play.api.ApplicationLoader.Context
import play.api.http.HeaderNames
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.Result
import play.api.test.{DefaultTestServerFactory, FakeRequest, RunningServer}
import play.api.test.Helpers.*
import tests.*

import scala.concurrent.Promise

trait ServerPerSuite2[T <: BuiltInComponents]:
  self: FunSuite =>
  def createComponents(context: Context): T
  lazy val serverComponents = createComponents(TestAppLoader.createTestAppContext)
  val testServer: Fixture[RunningServer] = new Fixture[RunningServer]("test-server"):
    private var runningServer: RunningServer = null
    def apply() = runningServer
    override def beforeAll(): Unit =
      runningServer = DefaultTestServerFactory.start(serverComponents.application)
    override def afterAll(): Unit =
      runningServer.stopServer.close()
  def port = testServer().endpoints.httpEndpoint.map(_.port).get

  override def munitFixtures: Seq[Fixture[?]] = Seq(testServer)

abstract class PimpcloudServerSuite extends FunSuite with ServerPerSuite2[TestComponents]:
  override def createComponents(context: Context): TestComponents = new TestComponents(context)

class IntegrationTest extends PimpcloudServerSuite with MusicPimpSuite:
  def cloudPort = port
  implicit val mat: Materializer = components.materializer
  def cloudHostPort = s"localhost:$cloudPort"
  def pimpcloudUri = FullUrl("ws", cloudHostPort, CloudSocket.path)
  def pimpOptions: InitOptions = TestOptions.default.copy(cloudUri = pimpcloudUri)
//  val musicpimp = new MusicPimpSuite(pimpOptions)
  def musicpimp = components
  def cloudClient = musicpimp.clouds
  def library = musicpimp.library
  def pimp = musicpimp.application
  val httpClient = AhcWSClient()
  val adminPath = "/admin/usage"
  val phonePath = "/ws/playback"
  val testTrackTitle = "Test of MP3 File"

  def cloud = testServer().app

  test("can do it"):
    assert(statusCode("/ping", pimp) == 200)
    assert(statusCode("/health", testServer().app) == 200)

  test("musicpimp can connect to pimpcloud"):
    try
      val expectedId = CloudID("connect-test")
      val id = await(cloudClient.connect(Option(expectedId)))
      assert(id == expectedId)
    finally cloudClient.disconnectAndForget("")

  test("server events"):
    val joinId = CloudID("join-test")
    val handler = new TestHandler
    val joinedPromise = Promise[PimpServer]()

    def onJson(json: Json) =
      val joinedServer = json.as[PimpServers].toOption.flatMap(_.servers.find(_.id == joinId))
      joinedServer.map(joinedPromise.success).getOrElse(handler.handle(json))

    try
      withPimpSocket(adminPath, onJson): client =>
        assert(client.isConnected)
        val result = await(handler.all())
        assert(result == 42)
        val id = await(cloudClient.connect(Option(joinId)))
        assert(id == joinId)
        val server = await(joinedPromise.future)
        assert(server.id == id)
    finally
      cloudClient.disconnectAndForget("")

  test("phone events"):
    try
      val expectedId = CloudID("phone-test")
      val id = await(cloudClient.connect(Option(expectedId)))
      assert(id == expectedId)
      val p = Promise[PimpPhone]()

      def onJson(json: Json): Unit =
        json
          .as[PimpPhones]
          .map(_.phones)
          .foreach: ps =>
            if ps.nonEmpty then p.trySuccess(ps.head)

      withPimpSocket(adminPath, onJson): adminSocket =>
        withPhoneSocket(phonePath, id, _ => ()): phoneSocket =>
          val joinedPhone = await(p.future)
          assert(joinedPhone.s == expectedId)
    finally cloudClient.disconnectAndForget("")

  test("stream events"):
    val p = Promise[String]()

    def onJson(json: Json): Unit =
      json
        .as[PimpStreams]
        .map(_.streams.map(_.track.title))
        .foreach: titles =>
          if titles.nonEmpty then p.success(titles.head)

    withCloudTrack("notification-test"): (trackId, _, cloudId) =>
      withPimpSocket(adminPath, onJson): _ =>
        val f = req(s"http://$cloudHostPort/tracks/$trackId", cloudId).get()
        val title = await(p.future)
        assert(title == testTrackTitle)
        await(f)

  test("serve entire track"):
    withCloudTrack("track-test"): (trackId, fileSize, cloudId) =>
      // request track
      val r = makeGet(s"/tracks/$trackId", cloudId)
      assert(r.status == 200)
      // It seems the content-length header is only set if the content is small enough for non-chunked encoding.
      // So, while this test passes also with this line uncommented, it's not representative.
      //      assert(r.header(HeaderNames.CONTENT_LENGTH).contains(fileSize.toBytes.toString))
      assert(r.bodyAsBytes.size.toLong == fileSize.toBytes)

  test("serve ranged track"):
    val bytesPromise = Promise[Int]()
    withCloudTrack("range-test"): (trackId, _, cloudId) =>
      // request track
      // the end of the range is inclusive
      val r = makeGet(s"/tracks/$trackId", cloudId, HeaderNames.RANGE -> s"bytes=10-20")
      assert(r.status == 206)
      assert(r.bodyAsBytes.size.toLong == 11)
      bytesPromise.success(r.bodyAsBytes.size)
    assert(await(bytesPromise.future) == 11)

  test("get folders"):
    withCloudTrack("folder-test"): (_, _, cloudId) =>
      val r = makeGet("/folders?f=json", cloudId)
      assert(r.status == 200)
      await(musicpimp.indexer.index().runWith(Sink.seq))
      val _ = makeGet("/folders?f=json", cloudId)
      val r3 = makeGet(s"/folders/Sv%C3%A5rt+%28%C3%A4r+det%29?f=json", cloudId)
      assert(r3.status == 200)

  test("get alarms"):
    withCloud("alarms-test"): cloudId =>
      val r = makeGet("/alarms?f=json", cloudId)
      assert(r.contentType == "application/json")
      assert(r.status == 200)

  test("search"):
    withCloud("search-test"): cloudId =>
      val r = makeGet("/search?term=iron&f=json", cloudId)
      assert(r.contentType == "application/json")
      assert(r.status == 200)

  override def munitFixtures: Seq[Fixture[?]] = Seq(testServer, testApp)

  class TestHandler:
    val requests = Promise[Json]()
    val phones = Promise[Json]()
    val servers = Promise[Json]()

    def handle(json: Json): Unit =
      json.as[PimpStreams].foreach(_ => requests.success(json))
      json.as[PimpPhones].foreach(_ => phones.success(json))
      json.as[PimpServers].foreach(_ => servers.success(json))

    def all() = for
      _ <- requests.future
      _ <- phones.future
      _ <- servers.future
    yield 42

  def withCloudTrack(desiredId: String)(code: (TrackID, StorageSize, CloudID) => Any) =
    // makes sure musicpimp server has a track to serve
    val trackFile = TestUtils.makeTestMp3()
    val fileSize = Files.size(trackFile).bytes
    assert(fileSize.toBytes == 198658L)
    val trackFolder = trackFile.getParent
    val created = Files.createDirectories(trackFolder.resolve("Svårt (är det)"))
    Files.createTempFile(created, "temp", ".mp3")
    library.setFolders(Seq(trackFolder))
    val _ = await(musicpimp.indexer.index().runWith(Sink.seq))
    val file = library.findAbsoluteNew(UnixPath(trackFile.getFileName))
    assert(file.isDefined)
    withCloud(desiredId): cloudId =>
      val trackId = Library.trackId(trackFile.getFileName)
      code(trackId, fileSize, cloudId)

  def withCloud(desiredId: String)(code: CloudID => Any) =
    try
      // connect to pimpcloud
      val cloudId = CloudID(desiredId)
      val id = await(cloudClient.connect(Option(cloudId)))
      assert(id == cloudId)
      code(id)
    finally cloudClient.disconnectAndForget("Test ended.")

  def makeGet(url: String, cloudId: CloudID, headers: (String, String)*) =
    await(req(s"http://$cloudHostPort$url", cloudId, headers*).get())

  def req(url: String, cloudId: CloudID, headers: (String, String)*) =
    val enc = cloudAuthorization(cloudId)
    val hs = headers :+ (HeaderNames.AUTHORIZATION -> s"Basic $enc")
    httpClient.url(url).withHttpHeaders(hs*)

  def cloudAuthorization(cloudId: CloudID) =
    Base64.encodeBase64String(s"$cloudId:admin:test".getBytes(StandardCharsets.UTF_8))

  def statusCode(uri: String, chosenApp: Application): Int =
    request(uri, chosenApp).header.status

  def request(uri: String, chosenApp: Application): Result =
    val result = route(chosenApp, FakeRequest(GET, uri)).get
    await(result)

  def withPhoneSocket[T](path: String, cloudId: CloudID, onMessage: Json => Any)(
    code: TestSocket => T
  ) =
    val authValue = s"Basic ${cloudAuthorization(cloudId)}"
    withCloudSocket(path, authValue, onMessage)(code)

  def withPimpSocket[T](path: String, onMessage: Json => Any)(code: TestSocket => T) =
    withCloudSocket(path, HttpUtil.authorizationValue("u", "p"), onMessage)(code)

  def withCloudSocket[T](path: String, authValue: String, onMessage: Json => Any)(
    code: TestSocket => T
  ) =
    val uri = new URI(s"ws://$cloudHostPort$path")
    Util.using(new TestSocket(uri, authValue, onMessage)): client =>
      await(client.initialConnection)
      code(client)

  class TestSocket(wsUri: URI, authValue: String, onJson: Json => Any)
    extends SocketClient(
      wsUri,
      SSLUtils.trustAllSslContext().getSocketFactory,
      Seq(HttpUtil.Authorization -> authValue)
    ):
    override def onText(message: String): Unit = onJson(message.asJson)

    def sendJson[C: Encoder](message: C) = send(message.asJson.noSpaces)
