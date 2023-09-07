package tests

import com.malliina.concurrent.Execution.cached
import com.malliina.http.FullUrl
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.audio.{TrackJson, TrackMeta}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models._
import com.malliina.storage.StorageInt
import com.malliina.values.UnixPath
import com.malliina.ws.HttpUtil
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.MimeTypes.JSON
import play.api.http.Writeable
import play.api.libs.json.{Format, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.duration.DurationInt

class PlaylistsTests extends munit.FunSuite with MusicPimpSuite {
  override def pimpOptions: InitOptions = TestOptions.default

  implicit val f: Format[TrackMeta] = TrackJson.format(FullUrl.build("http://www.google.com").toOption.get)
  val trackId = TrackID("Test.mp3")
  val testTracks: Seq[TrackID] = Seq(trackId)

  test("add tracks") {
    val lib = components.lib
    val db = lib.db
    val folderId = FolderID("Testid")
    import db._

    def trackInserts =
      lib.insertTracks(
        Seq(
          DataTrack(trackId, "Ti", "Ar", "Al", 10.seconds, 1.megs, UnixPath.Empty, folderId)
        )
      )

    val insertions = for {
      _ <- db.performAsync("Cleanup")(
        db.runIO(db.tracksTable.delete) *> db.runIO(db.foldersTable.delete)
      )
      foldersInserted <- lib.insertFolders(
        Seq(DataFolder(folderId, "Testfolder", UnixPath.Empty, folderId))
      )
      tracksInserted <- trackInserts
    } yield (foldersInserted, tracksInserted)
    val (fsi, tsi) = await(insertions)
    assert(fsi == 1)
    assert(tsi == 1)
    val maybeFolder = await(lib.folder(folderId))
    assert(maybeFolder.isDefined)
  }

  test("GET /playlists") {
    fetchLists()
    assert(1 == 1)
  }

  test("POST /playlists") {
    def postPlaylist(in: PlaylistSubmission) = {
      val response =
        fetch(
          FakeRequest("POST", "/playlists")
            .withJsonBody(Json.obj(JsonStrings.PlaylistKey -> Json.toJson(in)))
        )
      assert(status(response) == 202)
      (contentAsJson(response) \ "id").as[PlaylistID]
    }

    val submission = PlaylistSubmission(None, "test playlist", testTracks)
    val newId = postPlaylist(submission)
    assert(fetchLists().find(_.id == newId).get.tracks.map(_.id) == testTracks)
    val updatedTracks = testTracks ++ testTracks
    val updatedPlaylist = submission.copy(id = Option(newId), tracks = updatedTracks)
    val updatedId = postPlaylist(updatedPlaylist)
    assert(newId == updatedId)
    assert(fetchLists().find(_.id == updatedId).get.tracks.map(_.id) == updatedTracks)

    val response3 = fetch(FakeRequest(POST, s"/playlists/delete/$updatedId"))
    assert(status(response3) == 202)

    assert(fetchLists().isEmpty)
  }

  def fetchLists() = {
    val response = fetch(FakeRequest(GET, "/playlists"))
    assert(contentType(response) contains JSON)
    assert(status(response) == 200)
    (contentAsJson(response) \ "playlists").as[Seq[FullSavedPlaylist]]
  }

  def fetch[T: Writeable](request: FakeRequest[T]) =
    route(
      app,
      request.withHeaders(
        AUTHORIZATION -> HttpUtil.authorizationValue(
          NewUserManager.defaultUser.name,
          NewUserManager.defaultPass.pass
        ),
        ACCEPT -> JSON
      )
    ).get
}
