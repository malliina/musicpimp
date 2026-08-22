package tests

import cats.effect.IO
import com.malliina.http.UrlSyntax.url
import com.malliina.musicpimp.cloud.{Constants, CustomSSLSocketFactory, JsonSocket8}
import com.malliina.musicpimp.http.HttpConstants
import com.malliina.security.SSLUtils
import com.malliina.ws.HttpUtil

import javax.net.ssl.*

class WebSocketTests extends munit.CatsEffectSuite:
  test("can open socket".ignore):
    val factory = CustomSSLSocketFactory.forHost("cloud.musicpimp.org")
    openSocket(factory).map: _ =>
      assertEquals(1, 1)

  test("can open socket, without SNI".ignore):
    openSocket(SSLUtils.trustAllSslContext().getSocketFactory)

  def openSocket(socketFactory: SSLSocketFactory): IO[Unit] =
    JsonSocket8
      .default[IO](
        url"wss://cloud.musicpimp.org/servers/ws",
        socketFactory,
        HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("u", Constants.pass.pass)
      )
      .use: s =>
        s.connect()
