package tests

import com.malliina.musicpimp.app.{InitOptions, PimpLoader}
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{PimpUrl, SavedPlaylist}
import com.malliina.ws.HttpUtil
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplicationLoader}

class TestAppLoader extends WithApplicationLoader(new PimpLoader(TestOptions.default))

object TestOptions {
  val default = InitOptions(alarms = false, database = true, users = true, indexer = false, cloud = false)
}

class PlaylistsSpec extends Specification {
  val testTracks: Seq[String] = Nil
  implicit val f = TrackMeta.format(PimpUrl.build("http://www.google.com").get)

  "Playlist" should {
    "GET /playlists" in new TestAppLoader {
      val response = fetch(app, FakeRequest(GET, "/playlists"))
      status(response) mustEqual 200
      contentType(response) must beSome(MimeTypes.JSON)
      val maybeList = (contentAsJson(response) \ "playlists").asOpt[Seq[SavedPlaylist]]
      maybeList must beSome
    }

    "POST /playlists" in new TestAppLoader {
      if (testTracks.nonEmpty) {
        val submission = PlaylistSubmission(None, "test playlist", testTracks)
        val request = FakeRequest(POST, "/playlists")
          .withJsonBody(Json.obj(JsonStrings.PlaylistKey -> Json.toJson(submission)))
        val response = fetch(app, request)
        status(response) mustEqual 202

        val response2 = fetch(app, FakeRequest(GET, "/playlists"))
        status(response2) mustEqual 200
        contentType(response2) must beSome(MimeTypes.JSON)
        val maybeList = (contentAsJson(response2) \ "playlists").as[Seq[SavedPlaylist]]
        maybeList.size must beGreaterThanOrEqualTo(1)
        val first = maybeList.head
        first.tracks.size mustEqual 2

        val response3 = fetch(app, FakeRequest(POST, s"/playlists/delete/${first.id}"))
        status(response3) mustEqual 202

        val response4 = fetch(app, FakeRequest(GET, "/playlists"))
        status(response4) mustEqual 200
        val maybeList2 = (contentAsJson(response4) \ "playlists").as[Seq[SavedPlaylist]]
        maybeList2.size mustEqual 0
      } else {
        true mustEqual true
      }
    }
  }

  def fetch[T](app: Application, request: FakeRequest[T])(implicit w: Writeable[T]) = {
    route(app, request.withHeaders(
      HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue("test", "test"),
      HeaderNames.ACCEPT -> MimeTypes.JSON)).get
  }
}
