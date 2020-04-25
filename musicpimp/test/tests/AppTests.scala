package tests

import play.api.test.FakeRequest
import play.api.test.Helpers._

class AppTests extends AppSuite(TrivialAppLoader.components) {
  test("app starts") {
    val result = route(testApp().application, FakeRequest(GET, "/")).get
    assert(status(result) == 200)
  }
}
