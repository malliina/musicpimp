package com.malliina.musicpimp.auth

import com.malliina.util.AppLogger
import com.malliina.values.Literals.err
import com.malliina.values.{ErrorMessage, IdToken, Readable, TokenValue}
import com.malliina.web.{Expired, InvalidClaims, InvalidSignature, Issuer, JWTError, MissingData, ParseError}
import com.nimbusds.jose.crypto.{MACSigner, MACVerifier}
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jose.{JOSEException, JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

import java.text.ParseException
import java.time.Instant
import java.util.Date
import scala.concurrent.duration.{DurationLong, FiniteDuration}

object JWT:
  private val log = AppLogger(getClass)

  case class Parsed(
    token: TokenValue,
    jwt: SignedJWT,
    claims: JWTClaimsSet,
    claimsJson: Json
  ):
    val exp = Option(claims.getExpirationTime).map(_.toInstant)
    val iss = Option(claims.getIssuer).flatMap(s => Issuer.build(s).toOption)

    // No expiration => never expired
    def checkExpiration(now: Instant) = exp.flatMap: e =>
      if now.isBefore(e) then None else Option(Expired(token, e, now))

    def verify(secret: SecretKey, now: Instant): Either[JWTError, Verified] =
      Verified.verify(this, secret, now)

    def readString(key: String): Either[JWTError, String] =
      read(token, claims.getStringClaim(key), s"Claim missing: '$key'.")

  private object Parsed:
    def apply(token: TokenValue): Either[JWTError, Parsed] =
      for
        signed <- read(token, SignedJWT.parse(token.value), s"Invalid JWT: '$token'.")
        claims <- read(token, signed.getJWTClaimsSet, s"Missing claims: '$token'.")
        json <- parse(claims.toString).left.map: _ =>
          InvalidClaims(token, err"Claims must be JSON.")
      yield Parsed(token, signed, claims, json)

  case class Verified private (parsed: Parsed):
    def expiresIn(now: Instant): Option[FiniteDuration] = parsed.exp.map: exp =>
      (exp.toEpochMilli - now.toEpochMilli).millis
    def readString(key: String) = parsed.readString(key)
    def token = parsed.token
    def read[T: Decoder](key: String): Either[JWTError, T] =
      parsed.claimsJson.hcursor
        .downField(key)
        .as[T]
        .left
        .map: errors =>
          InvalidClaims(token, ErrorMessage(s"Invalid claims: '$errors'."))
    def parse[T](key: String)(using r: Readable[T]): Either[JWTError, T] =
      readString(key).flatMap: s =>
        r.read(s).left.map(err => InvalidClaims(token, err))

  object Verified:
    def verify(parsed: Parsed, secret: SecretKey, now: Instant): Either[JWTError, Verified] =
      val verifier = new MACVerifier(secret.value)
      val signatureVerification =
        try
          val verified = parsed.jwt.verify(verifier)
          if verified then Right(()) else Left(InvalidSignature(parsed.token))
        catch
          case je: JOSEException =>
            log.warn(s"JWT exception", je)
            Left(InvalidSignature(parsed.token))
      for
        _ <- signatureVerification
        _ <- parsed.checkExpiration(now).toLeft(())
      yield Verified(parsed)

  private def read[T](token: TokenValue, f: => T, onMissing: => String): Either[JWTError, T] =
    try Option(f).toRight(MissingData(token, ErrorMessage(onMissing)))
    catch
      case pe: ParseException =>
        log.error(s"Parse error for token '$token'.", pe)
        Left(ParseError(token, Option(pe), err"Parse error."))

class JWT(secret: SecretKey, dataKey: String = "data"):
  def sign[T: Encoder](payload: T, ttl: FiniteDuration, now: Instant = Instant.now()): IdToken =
    signWithExpiration[T](payload, now.plusSeconds(ttl.toSeconds))

  private def signWithExpiration[T: Encoder](payload: T, expiresAt: Instant): IdToken =
    val signer = new MACSigner(secret.value)
    val claims = new JWTClaimsSet.Builder()
      .expirationTime(Date.from(expiresAt))
      .claim(dataKey, JSONObjectUtils.parse(payload.asJson.noSpaces))
      .build()
    val signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims)
    signed.sign(signer)
    IdToken.unsafe(signed.serialize())

  def verify[T: Decoder](token: TokenValue, now: Instant = Instant.now()): Either[JWTError, T] =
    verifyToken(token, now).flatMap: v =>
      v.read[T](dataKey)

  private def verifyToken(token: TokenValue, now: Instant): Either[JWTError, JWT.Verified] =
    JWT
      .Parsed(token)
      .flatMap: p =>
        p.verify(secret, now)
