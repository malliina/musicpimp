package tests

import java.net.URL
import javax.net.ssl._

import com.malliina.musicpimp.cloud.JsonSocket8
import com.malliina.musicpimp.http.{CustomSSLSocketFactory, HttpConstants}
import com.malliina.musicpimp.models.PimpUrl
import com.malliina.security.SSLUtils
import com.malliina.ws.HttpUtil
import org.scalatest.FunSuite

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WebSocketTests extends FunSuite {
  test("can open socket") {
    val ctx = SSLUtils.trustAllSslContext()
    val matcher = SNIHostName.createSNIMatcher("cloud\\.musicpimp\\.org")
    val matchers = Seq(matcher)
    val inner = ctx.getSocketFactory
    val sslParameters = new SSLParameters()
    sslParameters.setSNIMatchers(matchers)
    val url = new URL("https://cloud.musicpimp.org")
    sslParameters.setServerNames(Seq(new SNIHostName(url.getHost)))
    val factory = new CustomSSLSocketFactory(inner, sslParameters)
    val s = new JsonSocket8(
      PimpUrl.build("wss://cloud.musicpimp.org/servers/ws2").get,
      factory,
      HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("u", "p"))
    val _ = Await.result(s.connect(), 5.seconds)
  }

  test("can open connection") {
    //    SNIHostName.createSNIMatcher()
    //    SSLUtils.trustAllSslContext().
  }
}

