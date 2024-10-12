package com.malliina.web

import java.time.Instant

import cats.effect.IO
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.TokenValue
import com.malliina.json.SharedPlayFormats.decoder

class KeyClient(val knownUrl: FullUrl, validator: TokenValidator, val http: HttpClient[IO]) {
  def validate(token: TokenValue, now: Instant = Instant.now()): IO[Either[AuthError, Verified]] =
    fetchKeys().map { keys => validator.validate(token, keys, now) }

  def fetchKeys(): IO[Seq[KeyConf]] =
    for {
      conf <- http.getAs[AuthEndpoints](knownUrl)
      keys <- http.getAs[JWTKeys](conf.jwksUri)
    } yield keys.keys
}
