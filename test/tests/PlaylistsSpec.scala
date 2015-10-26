package tests

import com.mle.musicpimp.app.{InitOptions, PimpLoader}
import com.mle.musicpimp.library.SavedPlaylist
import com.mle.ws.HttpUtil
import org.specs2.mutable.Specification
import play.api.http.HeaderNames
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
  "Playlist" should {
    "work" in new WithApp {
      val response = route(FakeRequest(GET, "/playlists")
        .withHeaders(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue("test", "test")))
        .get
      status(response) mustEqual 200
      contentType(response) must beSome("application/json")
      val maybeList = (contentAsJson(response) \ "playlists").asOpt[Seq[SavedPlaylist]]
      maybeList must beSome
    }
  }
}
