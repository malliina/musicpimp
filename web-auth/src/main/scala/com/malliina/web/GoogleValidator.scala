package com.malliina.web

import java.time.Instant

object GoogleValidator {
  val issuers = Seq("https://accounts.google.com", "accounts.google.com").map(Issuer.apply)

  def apply(clientIds: Seq[ClientId]): GoogleValidator = new GoogleValidator(clientIds, issuers)
}

class GoogleValidator(clientIds: Seq[ClientId], issuers: Seq[Issuer])
  extends TokenValidator(issuers) {
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    checkContains(Aud, clientIds.map(_.value), parsed).map { _ => parsed }
}
