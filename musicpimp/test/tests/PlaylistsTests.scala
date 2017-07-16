package tests

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.app.{InitOptions, PimpComponents}
import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.db.{DataFolder, DataTrack, DatabaseUserManager, PimpDb}
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models._
import com.malliina.play.http.FullUrl
import com.malliina.storage.StorageInt
import com.malliina.ws.HttpUtil
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.MimeTypes.JSON
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.duration.DurationInt

object TestOptions {
  val default = InitOptions(alarms = false, database = true, users = true, indexer = false, cloud = false)
}

class MusicPimpSuite(options: InitOptions = TestOptions.default)
  extends AppSuite(ctx => new PimpComponents(ctx, options, PimpDb.test()))

class PlaylistsTests extends MusicPimpSuite {
  implicit val f = TrackJson.format(FullUrl.build("http://www.google.com").get)
  val trackId = TrackID("Test.mp3")
  val testTracks: Seq[TrackID] = Seq(trackId)

  test("add tracks") {
    val db = components.deps.db
    val folderId = FolderID("Testid")

    def trackInserts = db.insertTracks(Seq(
      DataTrack(trackId, "Ti", "Ar", "Al", 10.seconds, 1.megs, folderId)
    ))

    val insertions = for {
      _ <- db.insertFolders(Seq(DataFolder(folderId, "Testfolder", PimpPath.Empty, folderId)))
      _ <- trackInserts
    } yield ()
    await(insertions)
    val maybeFolder = await(components.lib.folder(folderId))
    assert(maybeFolder.isDefined)
    val ts = maybeFolder.get.tracks
    ts foreach println
  }

  test("GET /playlists") {
    fetchLists()
    assert(1 === 1)
  }

  test("POST /playlists") {
    def postPlaylist(in: PlaylistSubmission) = {
      val response = fetch(FakeRequest("POST", "/playlists").withJsonBody(Json.obj(JsonStrings.PlaylistKey -> Json.toJson(in))))
      assert(status(response) === 202)
      (contentAsJson(response) \ "id").as[PlaylistID]
    }

    val submission = PlaylistSubmission(None, "test playlist", testTracks)
    val newId = postPlaylist(submission)
    assert(fetchLists().find(_.id == newId).get.tracks.map(_.id) === testTracks)
    val updatedTracks = testTracks ++ testTracks
    val updatedPlaylist = submission.copy(id = Option(newId), tracks = updatedTracks)
    val updatedId = postPlaylist(updatedPlaylist)
    assert(newId === updatedId)
    assert(fetchLists().find(_.id == updatedId).get.tracks.map(_.id) === updatedTracks)

    val response3 = fetch(FakeRequest(POST, s"/playlists/delete/$updatedId"))
    assert(status(response3) === 202)

    assert(fetchLists().isEmpty)
  }

  def fetchLists() = {
    val response = fetch(FakeRequest(GET, "/playlists"))
    assert(contentType(response) contains JSON)
    assert(status(response) === 200)
    (contentAsJson(response) \ "playlists").as[Seq[SavedPlaylist]]
  }

  def fetch[T: Writeable](request: FakeRequest[T]) = {
    route(app, request.withHeaders(
      AUTHORIZATION -> HttpUtil.authorizationValue(DatabaseUserManager.DefaultUser.name, DatabaseUserManager.DefaultPass.pass),
      ACCEPT -> JSON)).get
  }
}
