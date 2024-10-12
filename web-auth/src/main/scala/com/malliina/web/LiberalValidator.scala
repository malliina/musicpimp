package com.malliina.web

import java.time.Instant

import com.malliina.values.AccessToken

/** Accepts any claims, provides user as-is. */
class LiberalValidator(conf: KeyConf, issuer: Issuer)
  extends StaticTokenValidator[AccessToken, Verified](Seq(conf), issuer) {
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    Right(parsed)

  override protected def toUser(v: Verified) =
    Right(v)
}
