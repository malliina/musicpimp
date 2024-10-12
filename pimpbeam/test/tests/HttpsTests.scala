package tests

import org.apache.pekko.actor.ActorSystem
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._

class HttpsTests extends munit.FunSuite {
  val testTrustStorePassword = "???"

  implicit val as: ActorSystem = ActorSystem("test")
  implicit val ec: ExecutionContextExecutor = as.dispatcher

  test("can ping MusicBeamer over HTTP".ignore) {
    pingWithPlayAPI("https://beam.musicpimp.org/ping")
  }

  test("ping MusicBeamer over HTTPS with Apache HttpClient".ignore) {
    pingWithApacheHttpClient()
  }

//  test("can ping MusicBeamer over HTTPS with Apache HttpClient and modified socket factory") {
//    val fac = ApacheHttpHelper.allowAllCertificatesSocketFactory()
//    val client = HttpClientBuilder.create().setSSLSocketFactory(fac).build()
//    val req = new HttpGet("https://beam.musicpimp.org/ping")
//    val response = client.execute(req)
//    val responseContent = Option(response.getEntity) map EntityUtils.toString getOrElse "No response content"
//    assert(responseContent contains "version")
//  }

  private def pingWithApacheHttpClient(): Unit = {
    val client = HttpClientBuilder.create().build()
    val req = new HttpGet("https://beam.musicpimp.org/ping")
    val response = client.execute(req)
    val responseContent =
      Option(response.getEntity).map(EntityUtils.toString).getOrElse("No response content")
    assert(responseContent.contains("version"))
  }

  private def pingWithPlayAPI(url: String): WSResponse = {
    val client = AhcWSClient()
    val response = client.url(url).get()
    response.onComplete(_ => client.close())
    Await.result(response, 5.seconds)
  }
}
