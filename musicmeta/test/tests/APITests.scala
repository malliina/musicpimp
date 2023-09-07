package tests

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.malliina.musicmeta.MetaHtml
import com.malliina.oauth.{DiscoGsOAuthCredentials, GoogleOAuthCredentials}
import com.malliina.play.ActorExecution
import controllers.{Covers, MetaOAuth, MetaOAuthControl}
import play.api.{Mode, http}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

object APITests {
  val fakeCreds = DiscoGsOAuthCredentials("key", "secret", "token", "secret")
  val fakeGoogle = GoogleOAuthCredentials("client", "secret", "scope")
}

class APITests extends munit.FunSuite {
  implicit val timeout: FiniteDuration = 20.seconds
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val mat = Materializer.matFromSystem(actorSystem)
  val oauthControl =
    new MetaOAuthControl(stubControllerComponents().actionBuilder, APITests.fakeGoogle)
  val exec = ActorExecution(actorSystem, mat)
  val oauth = MetaOAuth(
    "username",
    MetaHtml("musicmeta-frontend", Mode.Test),
    stubControllerComponents().actionBuilder,
    exec
  )
  val covers = new Covers(oauth, APITests.fakeCreds, stubControllerComponents())

  test("respond to ping") {
    verifyActionResponse(covers.ping, OK)
  }

  test("proper cover search".ignore) {
    verifyActionResponse(
      covers.cover,
      OK,
      FakeRequest(GET, "/covers?artist=iron%20maiden&album=powerslave")
    )
  }

  test("nonexistent cover return 404".ignore) {
    verifyActionResponse(
      covers.cover,
      NOT_FOUND,
      FakeRequest(GET, "/covers?artist=zyz&album=abcde")
    )
  }

  test("invalid request returns HTTP 400 BAD REQUEST") {
    verifyActionResponse(covers.cover, http.Status.BAD_REQUEST)
  }

  private def verifyActionResponse(
    action: EssentialAction,
    expectedStatus: Int,
    req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  ): Unit = {
    verifyResponse(action(req).run(), expectedStatus)
  }

  private def verifyResponse(result: Future[Result], expectedStatus: Int = http.Status.OK): Unit = {
    val statusCode = Await.result(result, timeout).header.status
    assert(statusCode == expectedStatus)
  }
}
