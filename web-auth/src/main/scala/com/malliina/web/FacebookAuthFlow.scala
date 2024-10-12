package com.malliina.web

import cats.effect.IO
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.Email
import com.malliina.web.Utils.{stringify, urlEncode}
import com.malliina.json.SharedPlayFormats.decoder

object FacebookAuthFlow {
  def staticConf(conf: AuthConf) = StaticConf(
    "public_profile email",
    FullUrl.https("www.facebook.com", "/v6.0/dialog/oauth"),
    FullUrl.https("graph.facebook.com", "/v6.0/oauth/access_token"),
    conf
  )
}

class FacebookAuthFlow(authConf: AuthConf, http: HttpClient[IO])
  extends StaticFlowStart
  with CallbackValidator[Email] {
  val brandName = "Facebook"
  val conf = FacebookAuthFlow.staticConf(authConf)

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): IO[Either[AuthError, Email]] = {
    val params = validationParams(code, redirectUrl, authConf).map {
      case (k, v) => k -> urlEncode(v)
    }
    val url = conf.tokenEndpoint.append(s"?${stringify(params)}")

    // https://developers.facebook.com/docs/graph-api/explorer/
    for {
      tokens <- http.getAs[FacebookTokens](url)
      emailUrl = FullUrl.https(
        "graph.facebook.com",
        s"/v6.0/me?fields=email&access_token=${tokens.accessToken}"
      )
      emailResponse <- http.getAs[EmailResponse](emailUrl)
    } yield Right(emailResponse.email)
  }
}
