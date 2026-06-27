package com.malliina.musicpimp.auth

import cats.effect.Sync
import com.malliina.values.IdToken
import io.circe.*
import org.http4s.Credentials.Token
import org.http4s.headers.{Authorization, Cookie}
import org.http4s.{HttpDate, Request, Response, ResponseCookie, Uri}

import scala.concurrent.duration.DurationInt

class Http4sAuth[F[_]: Sync](
  val jwt: JWT,
  val cookieNames: CookieConf = CookieConf("pimp-user", "pimp-intended-uri")
) extends SyncAuthenticator[F, UserPayload]
  with UserAuthenticator[F]:
  private val cookiePath = Option("/")

  def auth(req: Request[F]): Either[Http4sAuthFailure, UserPayload] =
    readUser(cookieNames.user, req)

  def token(req: Request[?]) = req.headers
    .get[Authorization]
    .toRight(MissingCredentials(req))
    .flatMap(_.credentials match
      case Token(_, token) =>
        IdToken.build(token).left.map(err => MissingCredentials(req))
      case _ => Left(MissingCredentials(req)))

  def clearSession(res: Response[F]): res.Self =
    res
      .removeCookie(ResponseCookie(cookieNames.user, "", path = cookiePath))

  def withUser[T: Encoder](t: T, isSecure: Boolean, res: Response[F]): res.Self =
    withJwt(cookieNames.user, t, isSecure, res)

  def readIntendedUri(req: Request[F]): Option[Uri] =
    req.cookies
      .find(_.name == cookieNames.intendedUri)
      .flatMap(c => Uri.fromString(c.content).toOption)

  def withIntendedUri(uri: Uri, res: Response[F]): res.Self =
    res.addCookie(responseCookie(cookieNames.intendedUri, uri.renderString))

  def withJwt[T: Encoder](
    cookieName: String,
    t: T,
    isSecure: Boolean,
    res: Response[F]
  ): res.Self =
    val signed = jwt.sign[T](t, 12.hours)
    res.addCookie(
      ResponseCookie(
        cookieName,
        signed.value,
        httpOnly = true,
        secure = isSecure,
        path = cookiePath
      )
    )

  private def responseCookie(name: String, value: String) = ResponseCookie(
    name,
    value,
    Option(HttpDate.MaxValue),
    path = cookiePath,
    secure = true,
    httpOnly = true
  )

  private def readUser(
    cookieName: String,
    req: Request[F]
  ): Either[Http4sAuthFailure, UserPayload] =
    read[UserPayload](cookieName, req)

  def read[T: Decoder](cookieName: String, req: Request[F]): Either[Http4sAuthFailure, T] =
    for
      idToken <- readToken(cookieName, req)
      t <- jwt
        .verify[T](idToken)
        .left
        .map: err =>
          InvalidCredentials(req)
    yield t

  private def readToken(cookieName: String, req: Request[F]): Either[Http4sAuthFailure, IdToken] =
    for
      header <- req.headers.get[Cookie].toRight(InvalidCredentials(req))
      idToken <-
        header.values
          .find(_.name == cookieName)
          .flatMap(c => IdToken.build(c.content).toOption)
          .toRight(MissingCredentials(req))
    yield idToken
