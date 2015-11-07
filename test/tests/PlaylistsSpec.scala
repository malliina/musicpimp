package tests

import com.mle.musicpimp.app.{InitOptions, PimpLoader}
import com.mle.musicpimp.json.JsonStrings
import com.mle.musicpimp.library.PlaylistSubmission
import com.mle.musicpimp.models.SavedPlaylist
import com.mle.ws.HttpUtil
import org.specs2.mutable.Specification
import play.api.http.{HeaderNames, Writeable}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplicationLoader}

class WithApp extends WithApplicationLoader(new PimpLoader(TestOptions.default))

object TestOptions {
  val default = InitOptions(alarms = false, database = true, users = true, indexer = false, cloud = false)
}

/**
 * @author mle
 */
class PlaylistsSpec extends Specification {
  val testTracks: Seq[String] = Nil
  "Playlist" should {
    "GET /playlists" in new WithApp {
      val response = fetch(FakeRequest(GET, "/playlists"))
      status(response) mustEqual 200
      contentType(response) must beSome("application/json")
      val maybeList = (contentAsJson(response) \ "playlists").asOpt[Seq[SavedPlaylist]]
      maybeList must beSome
    }

    "POST /playlists" in new WithApp {
      if (testTracks.nonEmpty) {
        val submission = PlaylistSubmission(None, "test playlist", testTracks)
        val request = FakeRequest(POST, "/playlists").withJsonBody(Json.obj(JsonStrings.PlaylistKey -> Json.toJson(submission)))
        val response = fetch(request)
        status(response) mustEqual 202

        val response2 = fetch(FakeRequest(GET, "/playlists"))
        status(response2) mustEqual 200
        contentType(response2) must beSome("application/json")
        val maybeList = (contentAsJson(response2) \ "playlists").as[Seq[SavedPlaylist]]
        maybeList.size must beGreaterThanOrEqualTo(1)
        val first = maybeList.head
        first.tracks.size mustEqual 2

        val response3 = fetch(FakeRequest(POST, s"/playlists/delete/${first.id}"))
        status(response3) mustEqual 202

        val response4 = fetch(FakeRequest(GET, "/playlists"))
        status(response4) mustEqual 200
        val maybeList2 = (contentAsJson(response4) \ "playlists").as[Seq[SavedPlaylist]]
        maybeList2.size mustEqual 0
      } else {
        true mustEqual true
      }
    }
  }

  def fetch[T](request: FakeRequest[T])(implicit w: Writeable[T]) = {
    route(request.withHeaders(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue("test", "test"))).get
  }
}
