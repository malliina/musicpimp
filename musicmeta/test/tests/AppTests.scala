package tests

import com.malliina.musicmeta.AppComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AppTests extends AppSuite(new AppComponents(_, _ => APITests.fakeCreds, _ => APITests.fakeGoogle)) {
  test("router.ping") {
    val result = getRequest("/ping")
    assert(status(result) === 200)
  }

  test("request to nonexistent URL returns 404") {
    val result = getRequest("/ping2")
    assert(status(result) === 404)
  }

  test("router.badrequest") {
    val result = getRequest("/covers?artist=abba&album=")
    assert(status(result) === 400)
  }

  private def getRequest(path: String) =
    route(app, FakeRequest(GET, path)).get
}
