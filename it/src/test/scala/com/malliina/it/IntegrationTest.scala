package com.malliina.it

import cats.effect.IO
import com.malliina.http.{FullUrl, HttpClient, HttpHeaders}
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.cloud.CloudSocket
import com.malliina.musicpimp.http4s.PimpServerSuite
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{CloudID, TrackID}
import com.malliina.pimpcloud.http4s.CloudServerSuite
import com.malliina.pimpcloud.{PimpPhone, PimpPhones, PimpServer, PimpServers, PimpStreams}
import com.malliina.security.SSLUtils
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.Util
import com.malliina.values.UnixPath
import com.malliina.web.HttpConstants
import com.malliina.ws.HttpUtil
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import munit.{AnyFixture, FunSuite}
import org.apache.commons.codec.binary.Base64
import tests.*

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.concurrent.Promise

class IntegrationTest extends munit.CatsEffectSuite with CloudServerSuite with PimpServerSuite:
  def cloudPort: Int = ???
  def cloud = ??? // testServer().app
  def pimpcloudHostPort = s"localhost:$cloudPort"
  def cloudHostPort = FullUrl("http", pimpcloudHostPort, "")
  def pimpcloudUri = FullUrl("ws", s"localhost:$cloudPort", CloudSocket.path)
  def pimpOptions: InitOptions = TestOptions.default.copy(cloudUri = pimpcloudUri)
//  val musicpimp = new MusicPimpSuite(pimpOptions)
  def musicpimp = server().service
//  def musicpimp = components
  def cloudClient = musicpimp.clouds
  def library = musicpimp.files
//  def pimp = musicpimp.application
  val adminPath = "/admin/usage"
  val phonePath = "/ws/playback"
  val testTrackTitle = "Test of MP3 File"

  http.test("can do it"): client =>
    assertIO(client.get(server().baseHttpUrl.append("/ping")).map(_.status), 200)
    assertEquals(statusCode("/health", cloud), 200)

  test("musicpimp can connect to pimpcloud"):
    val expectedId = CloudID("connect-test")
    cloudClient
      .connect(Option(expectedId))
      .map: cid =>
        assertEquals(cid, expectedId)
      .attemptTap: _ =>
        cloudClient.disconnectAndForget("")

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
        assertEquals(result, 42)
        cloudClient.connect(Option(joinId))
        val id = await(cloudClient.connect(Option(joinId)).unsafeToFuture())
        assertEquals(id, joinId)
        val server = await(joinedPromise.future)
        IO.delay:
          assertEquals(server.id, id)
    finally
      cloudClient.disconnectAndForget("")

  test("phone events"):
    try
      val expectedId = CloudID("phone-test")
      val id = cloudClient.connect(Option(expectedId)).unsafeRunSync()
      assertEquals(id, expectedId)
      val p = Promise[PimpPhone]()

      def onJson(json: Json): Unit =
        json
          .as[PimpPhones]
          .map(_.phones)
          .foreach: ps =>
            if ps.nonEmpty then p.trySuccess(ps.head)

      withPimpSocket(adminPath, onJson): adminSocket =>
        IO.delay:
          withPhoneSocket(phonePath, id, _ => ()): phoneSocket =>
            val joinedPhone = await(p.future)
            IO.delay:
              assertEquals(joinedPhone.s, expectedId)
    finally cloudClient.disconnectAndForget("")

  http.test("stream events"): client =>
    val p = Promise[String]()

    def onJson(json: Json): Unit =
      json
        .as[PimpStreams]
        .map(_.streams.map(_.track.title))
        .foreach: titles =>
          if titles.nonEmpty then p.success(titles.head)

    withCloudTrack("notification-test"): (trackId, _, cloudId) =>
      withPimpSocket(adminPath, onJson): _ =>
        req(client, cloudHostPort.append(s"/tracks/$trackId"), cloudId).map: res =>
          val title = await(p.future)
          assertEquals(title, testTrackTitle)
          IO.unit

  http.test("serve entire track"): client =>
    withCloudTrack("track-test"): (trackId, fileSize, cloudId) =>
      // request track
      makeGet(client, s"/tracks/$trackId", cloudId).map: r =>
        assertEquals(r.status, 200)
        // It seems the content-length header is only set if the content is small enough for non-chunked encoding.
        // So, while this test passes also with this line uncommented, it's not representative.
        //      assert(r.header(HeaderNames.CONTENT_LENGTH).contains(fileSize.toBytes.toString))
        assertEquals(r.body.length.toLong, fileSize.toBytes)

  http.test("serve ranged track"): client =>
    val bytesPromise = Promise[Int]()
    val req = withCloudTrack("range-test"): (trackId, _, cloudId) =>
      // request track
      // the end of the range is inclusive
      makeGet(client, s"/tracks/$trackId", cloudId, "Range" -> s"bytes=10-20").map: r =>
        assertEquals(r.status, 206)
        assertEquals(r.body.length.toLong, 11L)
        bytesPromise.success(r.body.length)
    req.flatMap(_ => IO.delay(assertEquals(await(bytesPromise.future), 11)))

  http.test("get folders"): client =>
    withCloudTrack("folder-test"): (_, _, cloudId) =>
      for
        r <- makeGet(client, "/folders?f=json", cloudId)
        _ = assertEquals(r.status, 200)
        _ <- musicpimp.indexer.submitIndexAndSave()
        _ <- makeGet(client, "/folders?f=json", cloudId)
        r3 <- makeGet(client, s"/folders/Sv%C3%A5rt+%28%C3%A4r+det%29?f=json", cloudId)
      yield assertEquals(r3.status, 200)

  http.test("get alarms"): client =>
    withCloud("alarms-test"): cloudId =>
      makeGet(client, "/alarms?f=json", cloudId).map: r =>
        assertEquals(
          r.headers.get(HttpHeaders.`Content-Type`).flatMap(_.headOption).getOrElse(""),
          "application/json"
        )
        assertEquals(r.status, 200)

  http.test("search"): client =>
    withCloud("search-test"): cloudId =>
      makeGet(client, "/search?term=iron&f=json", cloudId).map: r =>
        assertEquals(
          r.headers.get(HttpHeaders.`Content-Type`).flatMap(_.headOption).getOrElse(""),
          "application/json"
        )
        assertEquals(r.status, 200)

  override def munitFixtures: Seq[AnyFixture[?]] = Seq(cloudServer, server)

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

  def withCloudTrack(desiredId: String)(code: (TrackID, StorageSize, CloudID) => IO[Any]) =
    // makes sure musicpimp server has a track to serve
    val trackFile = TestUtils.makeTestMp3()
    val fileSize = Files.size(trackFile).bytes
    assert(fileSize.toBytes == 198658L)
    val trackFolder = trackFile.getParent
    val created = Files.createDirectories(trackFolder.resolve("Svårt (är det)"))
    Files.createTempFile(created, "temp", ".mp3")
    library.setFolders(Seq(trackFolder))
    musicpimp.indexer.submitIndexAndSave().unsafeRunSync()
    val file = library.findAbsoluteNew(UnixPath(trackFile.getFileName))
    assert(file.isDefined)
    withCloud(desiredId): cloudId =>
      val trackId = Library.trackId(trackFile.getFileName)
      code(trackId, fileSize, cloudId)

  def withCloud(desiredId: String)(code: CloudID => IO[Any]): IO[Any] =
    try
      // connect to pimpcloud
      val cloudId = CloudID(desiredId)
      val id = cloudClient.connect(Option(cloudId)).unsafeRunSync()
      assertEquals(id, cloudId)
      code(id)
    finally cloudClient.disconnectAndForget("Test ended.")

  def makeGet(http: HttpClient[IO], path: String, cloudId: CloudID, headers: (String, String)*) =
    req(http, cloudHostPort.append(path), cloudId, headers*)

  def req(http: HttpClient[IO], url: FullUrl, cloudId: CloudID, headers: (String, String)*) =
    val enc = cloudAuthorization(cloudId)
    val hs = headers :+ (HttpHeaders.Authorization -> s"Basic $enc")
    http.get(url, hs.toMap)

  def cloudAuthorization(cloudId: CloudID) =
    Base64.encodeBase64String(s"$cloudId:admin:test".getBytes(StandardCharsets.UTF_8))

  def statusCode(uri: String, chosenApp: String): Int =
    ???
//    request(uri, chosenApp).header.status

  def withPhoneSocket[T](path: String, cloudId: CloudID, onMessage: Json => Any)(
    code: TestSocket => IO[T]
  ) =
    val authValue = s"Basic ${cloudAuthorization(cloudId)}"
    withCloudSocket(path, authValue, onMessage)(code)

  def withPimpSocket[T](path: String, onMessage: Json => Any)(code: TestSocket => IO[T]) =
    withCloudSocket(path, HttpUtil.authorizationValue("u", "p"), onMessage)(code)

  def withCloudSocket[T](path: String, authValue: String, onMessage: Json => Any)(
    code: TestSocket => IO[T]
  ) =
    val uri = new URI(s"ws://$pimpcloudHostPort")
    Util.using(new TestSocket(uri, authValue, onMessage)): client =>
      await(client.initialConnection)
      code(client).unsafeRunSync()

  class TestSocket(wsUri: URI, authValue: String, onJson: Json => Any)
    extends SocketClient(
      wsUri,
      SSLUtils.trustAllSslContext().getSocketFactory,
      Seq(HttpUtil.Authorization -> authValue)
    ):
    override def onText(message: String): Unit = onJson(message.asJson)

    def sendJson[C: Encoder](message: C) = send(message.asJson.noSpaces)
