package tests

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.scalatest.FunSuite
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Await
import scala.concurrent.duration._

class HttpsTests extends FunSuite {
  val testTrustStorePassword = "???"

  val mat = ActorMaterializer()(ActorSystem("test"))
  implicit val ec = mat.executionContext

  ignore("can ping MusicBeamer over HTTP") {
    pingWithPlayAPI("https://beam.musicpimp.org/ping")
  }

  ignore("ping MusicBeamer over HTTPS with Apache HttpClient") {
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
    val responseContent = Option(response.getEntity) map EntityUtils.toString getOrElse "No response content"
    assert(responseContent contains "version")
  }

  private def pingWithPlayAPI(url: String): WSResponse = {
    val client = AhcWSClient()(mat)
    val response = client.url(url).get()
    response.onComplete(_ => client.close())
    Await.result(response, 5.seconds)
  }
}
