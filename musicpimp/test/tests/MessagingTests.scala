package tests

import com.malliina.musicpimp.messaging.adm.AdmClient
import com.malliina.musicpimp.messaging.cloud.{APNSRequest, CloudPushClient, PushTask}
import com.malliina.push.apns.{APNSMessage, APNSToken}
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class MessagingTests extends FunSuite {
  val testToken = APNSToken.build("6c9969eee832f6ed2a11d04d6daa404db13cc3d97f7298f0c042616fc2a5cc34").get
//  val testToken = APNSToken.build("193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9").get
  val testTask = PushTask(
    Option(
      APNSRequest(
        Seq(testToken),
        APNSMessage.badged("this is a test", badge = 43))),
    None,
    None,
    None
  )

  ignore("can retrieve access token") {
    val tokenFuture = AdmClient.accessToken
    val token = Await.result(tokenFuture, 5.seconds)
    assert(token.expires_in === 3600.seconds)
  }

  ignore("can push notification using pimpcloud") {
    val request = CloudPushClient.default.push(testTask)
    val result = Await.result(request, 5.seconds)
    assert(result.getStatusCode === 200)
    println(result.getResponseBody)
  }
}
