package tests

import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.adm.CloudAdmClient
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.musicpimp.messaging.gcm.CloudGcmClient
import com.malliina.push.adm.ADMToken
import com.malliina.push.apns.{APNSMessage, APNSToken}
import com.malliina.push.gcm.GCMToken
import com.malliina.push.mpns.{MPNSToken, ToastMessage}
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

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
    None,
    None
  )

  ignore("send using adm") {
    val server = ServerTag("523f7e6d-003f-49e8-a2e2-f7bc712b1dfc")
    val id = ADMToken("amzn1.adm-registration.v3.Y29tLmFtYXpvbi5EZXZpY2VNZXNzYWdpbmcuUmVnaXN0cmF0aW9uSWRFbmNyeXB0aW9uS2V5ITEhaXJTVmVVQXhJSnhnRGs3MGl0S0E1TExldGwxcWFPRzFHK3cwL1N1OS9zMnN1RFFBZHd1VkNWMXhaYlp3dTExWGdTNytOYk5jaVZ1OEtKWnoyNmhubnBoYTdiRVhlTzJTYkd3TlFaWXBNMHRCSDRnZi9KQUl1VGtoUzBwdjVkU2gwdGs1R2RWaVRRYzRHWFF3ZUU5MTU2MHI2ODRxN0pCYnJSMVFyaHhPUjI4NmVPT0lUcG5SWjJxUnRrOWZMdFdJdlpWMWxCVk1MSmtkRmIxY3llMkZZRWo0WFpiVWxMUEsrbnduZFB0Rm80TUdubFYxK1ZNdDA2bGJ5NFozNTZnbCtJVXBPRG9maTZ1NktnZXR0akZEeXhSV1pRV0lDRWh6b2ROenNxRTRsa0poS3EvSjJzaXFPQWpuVzd2Z0tINjZvRTFUV0MvOVJDaU81bE9pQmtGY3RnPT0hZGQ5WjZ2Z1c1MVJxY2kva2NmbmhyZz09")
    val message = CloudAdmClient.message(server)
    val request = CloudPushClient.default.push(PushTask(adm = Option(ADMRequest(Seq(id), message))))
    val response = await(request)
    assert(response.getStatusCode === 200)
  }

  ignore("send using mpns or wns") {
    val token = MPNSToken("http://e.notify.live.net/u/1/db5/H2QAAAAcTAy_brVhkIlr8uosKoIeQNMVLg-SP76ym6iAGiUbD9WHm2eB7czfYH-voE-4ySAegav4KjeAuqNeLp8OnvEGE52klZNijMdoq554I8TAUS8EQt1uTBJqAw5BYWKIDMY/d2luZG93c3Bob25lZGVmYXVsdA/MJDNhFxKA0qwq01Zwvp9Qg/LftbTayvYfqUue0Q7AzWLo4t4d0")
    val tag = "b12c0460-fd45-4fd2-81a4-de50f349f0b6"
    val message = ToastMessage("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=$tag", silent = true)
    val request = CloudPushClient.default.push(PushTask(mpns = Option(MPNSRequest(tokens = Seq(token), toast = Option(message)))))
    val response = await(request)
    assert(response.getStatusCode === 200)
  }

  ignore("send using gcm") {
    val token = GCMToken("APA91bHR4yng5EXmeQZvl8MQB-t9_R33-0ScxK4U10Jtc9QPBg34s1biuA_sBJdOz6VsYcWDldPq8yUePbJV9k0TesQZCf1lgnQmgi7OohpBmlokw5TQ6OxBOKvtMBA5GSzr_QDQm7DCloQ5AnsW8gcPR_GUr_tcDg")
    val tag = ServerTag("9d0dc896-7883-44ff-830c-f02bae39535d")
    val task = PushTask(gcm = Option(GCMRequest(Seq(token), CloudGcmClient.message(tag))))
    val request = CloudPushClient.default.push(task)
    val response = await(request)
    assert(response.getStatusCode === 200)
    println(response.getResponseBody)
  }

  ignore("can push notification using pimpcloud") {
    val request = CloudPushClient.default.push(testTask)
    val result = Await.result(request, 5.seconds)
    assert(result.getStatusCode === 200)
    println(result.getResponseBody)
  }

  def await[T](f: Future[T]): T = Await.result(f, 10.seconds)
}
