package tests

import com.malliina.musicpimp.app.{InitOptions, PimpComponents}
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{PimpUrl, SavedPlaylist, TrackID}
import com.malliina.ws.HttpUtil
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

object TestOptions {
  val default = InitOptions(alarms = false, database = true, users = true, indexer = false, cloud = false)
}

class MusicPimpSuite extends AppSuite(ctx => new PimpComponents(ctx, TestOptions.default, PimpDb.test()))

class PlaylistsTests extends MusicPimpSuite {
  val testTracks: Seq[TrackID] = Nil
  implicit val f = TrackMeta.format(PimpUrl.build("http://www.google.com").get)

  ignore("GET /playlists") {
    val response = fetch(app, FakeRequest(GET, "/playlists"))
    assert(status(response) === 200)
    assert(contentType(response) contains MimeTypes.JSON)
    val maybeList = (contentAsJson(response) \ "playlists").asOpt[Seq[SavedPlaylist]]
    assert(maybeList.isDefined)
  }

  ignore("POST /playlists") {
    if (testTracks.nonEmpty) {
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
      assert(first.tracks.size === 2)

      val response3 = fetch(app, FakeRequest(POST, s"/playlists/delete/${first.id}"))
      assert(status(response3) === 202)

      val response4 = fetch(app, FakeRequest(GET, "/playlists"))
      assert(status(response4) === 200)
      val maybeList2 = (contentAsJson(response4) \ "playlists").as[Seq[SavedPlaylist]]
      assert(maybeList2.isEmpty)
    } else {
      assert(1 === 1)
    }
  }

  def fetch[T: Writeable](app: Application, request: FakeRequest[T]) = {
    route(app, request.withHeaders(
      HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue("test", "test"),
      HeaderNames.ACCEPT -> MimeTypes.JSON)).get
  }
}
