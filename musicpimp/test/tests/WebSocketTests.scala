package tests

import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.{Constants, CustomSSLSocketFactory, JsonSocket8}
import com.malliina.musicpimp.http.HttpConstants
import com.malliina.security.SSLUtils
import com.malliina.ws.HttpUtil

import javax.net.ssl.*
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WebSocketTests extends munit.FunSuite:
  test("can open socket".ignore):
    val factory = CustomSSLSocketFactory.forHost("cloud.musicpimp.org")
    openSocket(factory)

  test("can open socket, without SNI".ignore):
    openSocket(SSLUtils.trustAllSslContext().getSocketFactory)

  def openSocket(socketFactory: SSLSocketFactory) =
    val s = new JsonSocket8(
      FullUrl.build("wss://cloud.musicpimp.org/servers/ws").toOption.get,
      socketFactory,
      HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("u", Constants.pass.pass)
    )
    val _ = Await.result(s.connect(), 5.seconds)
