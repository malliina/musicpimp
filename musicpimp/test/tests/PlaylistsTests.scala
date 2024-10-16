package tests

import cats.effect.IO
import com.malliina.concurrent.Execution.runtime
import com.malliina.http.FullUrl
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.audio.{TrackJson, TrackMeta}
import com.malliina.musicpimp.db.*
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.*
import com.malliina.storage.StorageInt
import com.malliina.values.UnixPath
import com.malliina.ws.HttpUtil
import io.circe.Codec
import io.circe.parser
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.MimeTypes.JSON
import play.api.http.Writeable
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.libs.json.Json as PlayJson

import scala.concurrent.duration.DurationInt

class PlaylistsTests extends munit.FunSuite with MusicPimpSuite:
  override def pimpOptions: InitOptions = TestOptions.default

  implicit val f: Codec[TrackMeta] =
    TrackJson.format(FullUrl.build("http://www.google.com").toOption.get)
  val trackId = TrackID("Test.mp3")
  val testTracks: Seq[TrackID] = Seq(trackId)

  test("add tracks"):
    val lib: DatabaseLibrary[IO] = components.lib
    val folderId = FolderID("Testid")

    def trackInserts =
      lib.insertTracks(
        Seq(
          DataTrack(trackId, "Ti", "Ar", "Al", 10.seconds, 1.megs, UnixPath.Empty, folderId)
        )
      )

    val insertions = for
      _ <- lib.deleteTracks
      _ <- lib.deleteFolders
      foldersInserted <- lib.insertFolders(
        Seq(DataFolder(folderId, "Testfolder", UnixPath.Empty, folderId))
      )
      tracksInserted <- trackInserts
    yield (foldersInserted, tracksInserted)
    val (fsi, tsi) = insertions.unsafeRunSync()
    assertEquals(fsi, 1L)
    assertEquals(tsi, 1L)
    val maybeFolder = lib.folder(folderId).unsafeRunSync()
    assert(maybeFolder.isDefined)

  test("GET /playlists"):
    fetchLists()
    assertEquals(1, 1)

  test("POST /playlists"):
    import com.malliina.http.PlayCirce.writer
    def postPlaylist(in: PlaylistSubmission) =
      val response =
        fetch(
          FakeRequest("POST", "/playlists")
            .withJsonBody(PlayJson.obj(JsonStrings.PlaylistKey -> PlayJson.toJson(in)))
        )
      assert(status(response) == 202)
      parser
        .parse(contentAsString(response))
        .flatMap(_.hcursor.downField("id").as[PlaylistID])
        .fold(err => throw Exception(err.getMessage), identity)

    val submission = PlaylistSubmission(None, "test playlist", testTracks)
    val newId = postPlaylist(submission)
    val list = fetchLists()
    val added = list.find(_.id == newId)
    assert(added.get.tracks.map(_.id) == testTracks)
    val updatedTracks = testTracks ++ testTracks
    val updatedPlaylist = submission.copy(id = Option(newId), tracks = updatedTracks)
    val updatedId = postPlaylist(updatedPlaylist)
    assertEquals(newId, updatedId)
    assert(fetchLists().find(_.id == updatedId).get.tracks.map(_.id) == updatedTracks)

    val response3 = fetch(FakeRequest(POST, s"/playlists/delete/$updatedId"))
    assertEquals(status(response3), 202)

    assert(fetchLists().isEmpty)

  def fetchLists(): Seq[FullSavedPlaylist] =
    val response = fetch(FakeRequest(GET, "/playlists"))
    assert(contentType(response).contains(JSON))
    assert(status(response) == 200)
    parser
      .parse(contentAsString(response))
      .flatMap(_.hcursor.downField("playlists").as[Seq[FullSavedPlaylist]])
      .fold(err => throw Exception(err.getMessage), identity)

  def fetch[T: Writeable](request: FakeRequest[T]) =
    route(
      app,
      request.withHeaders(
        AUTHORIZATION -> HttpUtil.authorizationValue(
          DoobieUserManager.defaultUser.name,
          DoobieUserManager.defaultPass.pass
        ),
        ACCEPT -> JSON
      )
    ).get
