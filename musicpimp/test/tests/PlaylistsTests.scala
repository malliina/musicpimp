package tests

import com.malliina.musicpimp.app.{InitOptions, PimpComponents}
import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.db.{DataFolder, DataTrack, DatabaseUserManager, PimpDb}
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{FolderID, PimpPath, SavedPlaylist, TrackID}
import com.malliina.play.http.FullUrl
import com.malliina.storage.StorageInt
import com.malliina.ws.HttpUtil
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.duration.DurationInt
import com.malliina.concurrent.ExecutionContexts.cached

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
    val response = fetch(app, FakeRequest(GET, "/playlists"))
    assert(status(response) === 200)
    assert(contentType(response) contains MimeTypes.JSON)
    val maybeList = (contentAsJson(response) \ "playlists").asOpt[Seq[SavedPlaylist]]
    assert(maybeList.isDefined)
  }

  test("POST /playlists") {
    val submission = PlaylistSubmission(None, "test playlist", testTracks)
    val request = FakeRequest(POST, "/playlists")
      .withJsonBody(Json.obj(JsonStrings.PlaylistKey -> Json.toJson(submission)))
    val response = fetch(app, request)
    assert(status(response) === 202)

    val response2 = fetch(app, FakeRequest(GET, "/playlists"))
    assert(status(response2) === 200)
    assert(contentType(response2) contains MimeTypes.JSON)
    val maybeList = (contentAsJson(response2) \ "playlists").as[Seq[SavedPlaylist]]
    assert(maybeList.nonEmpty)
    val first = maybeList.head
    assert(first.tracks.size === testTracks.size)

    val response3 = fetch(app, FakeRequest(POST, s"/playlists/delete/${first.id}"))
    assert(status(response3) === 202)

    val response4 = fetch(app, FakeRequest(GET, "/playlists"))
    assert(status(response4) === 200)
    val maybeList2 = (contentAsJson(response4) \ "playlists").as[Seq[SavedPlaylist]]
    assert(maybeList2.isEmpty)
  }

  def fetch[T: Writeable](app: Application, request: FakeRequest[T]) = {
    route(app, request.withHeaders(
      HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue(DatabaseUserManager.DefaultUser.name, DatabaseUserManager.DefaultPass.pass),
      HeaderNames.ACCEPT -> MimeTypes.JSON)).get
  }
}
