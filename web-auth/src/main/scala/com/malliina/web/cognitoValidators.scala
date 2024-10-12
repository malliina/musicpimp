package com.malliina.web

import java.time.Instant

import com.malliina.values._

object CognitoValidator extends OAuthKeys {
  val Access = "access"
  val Id = "id"
  val TokenUse = "token_use"
  val UserKey = "username"
  val GroupsKey = "cognito:groups"
}

case class CognitoValidation(
  issuer: String,
  tokenUse: String,
  clientIdKey: String,
  clientId: String
)

abstract class CognitoValidator[T <: TokenValue, U](keys: Seq[KeyConf], issuer: Issuer)
  extends StaticTokenValidator[T, U](keys, issuer)

class CognitoAccessValidator(keys: Seq[KeyConf], issuer: Issuer, clientId: ClientId)
  extends CognitoValidator[AccessToken, CognitoUser](keys, issuer) {
  import com.malliina.web.CognitoValidator._

  protected def toUser(verified: Verified): Either[JWTError, CognitoUser] = {
    val jwt = verified.parsed
    for {
      username <- jwt
        .readString(UserKey)
        .filterOrElse(
          _.nonEmpty,
          InvalidClaims(jwt.token, ErrorMessage("Username must be non-empty."))
        )
      email <- jwt.readStringOpt(EmailKey)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    } yield CognitoUser(Username(username), email.map(Email.apply), groups, verified)
  }

  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkClaim(TokenUse, Access, parsed)
      _ <- checkClaim(ClientIdKey, clientId.value, parsed)
    } yield parsed
}

class CognitoIdValidator(keys: Seq[KeyConf], issuer: Issuer, val clientIds: Seq[ClientId])
  extends CognitoValidator[IdToken, CognitoUser](keys, issuer) {
  def this(keys: Seq[KeyConf], issuer: Issuer, clientId: ClientId) =
    this(keys, issuer, Seq(clientId))
  import com.malliina.web.CognitoValidator._

  override protected def toUser(verified: Verified): Either[JWTError, CognitoUser] = {
    val jwt = verified.parsed
    for {
      email <- jwt.readString(EmailKey).map(Email.apply)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    } yield CognitoUser(Username(email.email), Option(email), groups, verified)
  }

  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkClaim(TokenUse, Id, parsed)
      _ <- checkContains(Aud, clientIds.map(_.value), parsed)
    } yield parsed
}
