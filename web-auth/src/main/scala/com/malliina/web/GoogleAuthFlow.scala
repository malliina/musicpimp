package com.malliina.web

import cats.effect.IO
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.{Email, ErrorMessage}
import com.malliina.web.GoogleAuthFlow.EmailVerified
import com.malliina.web.OAuthKeys.EmailKey

object GoogleAuthFlow {
  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")
  val EmailVerified = "email_verified"

  def apply(conf: AuthCodeConf): GoogleAuthFlow = new GoogleAuthFlow(conf)
  def apply(creds: AuthConf, http: HttpClient[IO]): GoogleAuthFlow = apply(conf(creds, http))

  def conf(creds: AuthConf, http: HttpClient[IO]) = AuthCodeConf(
    "Google",
    creds,
    keyClient(Seq(creds.clientId), http),
    Map.empty
  )

  def keyClient(clientIds: Seq[ClientId], http: HttpClient[IO]): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientIds), http)
}

class GoogleAuthFlow(conf: AuthCodeConf)
  extends DiscoveringAuthFlow[Email](conf)
  with LoginHint[IO] {
  override def parse(validated: Verified): Either[JWTError, Email] = {
    val emailVerified = validated.readBoolean(EmailVerified)
    for {
      _ <- emailVerified.filterOrElse(
        _ == true,
        InvalidClaims(validated.token, ErrorMessage("Email not verified."))
      )
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
