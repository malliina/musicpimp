package tests

import javax.net.ssl._

import com.malliina.http.FullUrl
import com.malliina.logstreams.client.CustomSSLSocketFactory
import com.malliina.musicpimp.cloud.{Constants, JsonSocket8}
import com.malliina.musicpimp.http.HttpConstants
import com.malliina.security.SSLUtils
import com.malliina.ws.HttpUtil
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WebSocketTests extends FunSuite {
  ignore("can open socket") {
    val factory = CustomSSLSocketFactory.forHost("cloud.musicpimp.org")
    openSocket(factory)
  }

  ignore("can open socket, without SNI") {
    openSocket(SSLUtils.trustAllSslContext().getSocketFactory)
  }

  def openSocket(socketFactory: SSLSocketFactory) = {
    val s = new JsonSocket8(
      FullUrl.build("wss://cloud.musicpimp.org/servers/ws").right.get,
      socketFactory,
      HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("u", Constants.pass.pass))
    val _ = Await.result(s.connect(), 5.seconds)
  }
}
