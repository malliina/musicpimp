package tests

import org.specs2.mutable.Specification
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AppTests extends Specification {
  "App" should {
    "start" in new TrivialAppLoader {
      val result = route(app, FakeRequest(GET, "/")).get
      status(result) mustEqual 200
    }
  }
}
