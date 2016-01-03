package tests

import com.malliina.musicpimp.messaging.adm.AdmClient
import com.malliina.musicpimp.messaging.cloud.{APNSRequest, CloudPushClient, PushTask}
import com.malliina.push.apns.{APNSMessage, APNSToken}
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * @author Michael
  */
class MessagingTests extends FunSuite {
  val testToken = APNSToken.build("193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9").get
  val testTask = PushTask(
    Option(
      APNSRequest(
        Seq(testToken),
        APNSMessage.badged("this is a test", badge = 42))),
    None,
    None,
    None
  )

  test("can retrieve access token") {
    val tokenFuture = AdmClient.accessToken
    val token = Await.result(tokenFuture, 5.seconds)
    assert(token.expires_in === 3600.seconds)
  }

  test("can push notification using pimpcloud") {
    val result = CloudPushClient.default.push(testTask)
    Await.result(result, 5.seconds)
  }
}
