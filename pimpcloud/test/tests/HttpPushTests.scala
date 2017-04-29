package tests

import com.malliina.musicpimp.messaging.cloud.{APNSRequest, PushResult, PushTask}
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.pimpcloud.{AppConf, CloudComponents, NoPusher}
import com.malliina.play.auth.AuthFailure
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import com.malliina.push.apns.{APNSMessage, APNSToken}
import controllers.pimpcloud.{PimpAuth, Push}
import play.api.ApplicationLoader.Context
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class TestComponents(context: Context) extends CloudComponents(
  context,
  AppConf(NoPusher, GoogleOAuthCredentials("id", "secret", "scope"), _ => TestAuth)
)

object TestAuth extends PimpAuth {
  val testUser = Username("test")

  override def logged(action: EssentialAction) = action

  override def authenticate(request: RequestHeader): Future[Either[AuthFailure, AuthedRequest]] =
    Future.successful(Right(AuthedRequest(testUser, request)))

  override def authAction(f: AuthedRequest => Result) = Action { req =>
    val fakeRequest = AuthedRequest(testUser, req, None)
    f(fakeRequest)
  }
}

class PimpcloudSuite extends AppSuite(new TestComponents(_))

class HttpPushTests extends PimpcloudSuite {
  //  val tokenString = "193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9"
  val tokenString = "81bae54a590a3ae871408bd565d7e441aa952744770783209b2fd54219e3d9fe"
  val testToken = APNSToken.build(tokenString).get
  val testTask = PushTask(
    Option(
      APNSRequest(
        Seq(testToken),
        APNSMessage.badged("this is a test", badge = 4))),
    None,
    None,
    None,
    None
  )

  test("respond to health check") {
    val response = route(app, FakeRequest(GET, "/health")).get
    assert(status(response) === 200)
  }

  ignore("push a notification") {
    val body = Json.obj(Push.Cmd -> Push.PushValue, Push.Body -> testTask)
    val response = route(app, FakeRequest(POST, "/push").withJsonBody(body)).get
    val result = (contentAsJson(response) \ Push.ResultKey).as[PushResult]
    assert(result.apns.size === 2)
  }

  //    "get inactive devices" in {
  //      val conf = PushConfReader.load.apns
  //      val client = new APNSClient(conf.keyStore, conf.keyStorePass)
  //      import concurrent.duration.DurationInt
  //      val inactives = Await.result(client.inactiveDevices, 10.seconds)
  //      println(inactives)
  //      1 mustEqual 1
  //    }
}
