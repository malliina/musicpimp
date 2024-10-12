package com.malliina.play.auth

import com.malliina.play.auth.RememberMe._
import com.malliina.play.http.AuthedRequest
import com.malliina.values.Username
import play.api.Logger
import play.api.http.{JWTConfiguration, SecretConfiguration}
import play.api.libs.crypto.CookieSigner
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/** Adapted from https://github.com/wsargent/play20-rememberme
  */
object RememberMe {
  private val log = Logger(getClass)

  val CookieName = "REMEMBER_ME"
  val SeriesName = "series"
  val UserIdName = "userId"
  val TokenName = "token"
  val discardingCookie = DiscardingCookie(CookieName)
}

class RememberMe(
  store: TokenStore,
  val cookieSigner: CookieSigner,
  val secretConfiguration: SecretConfiguration
)(implicit ec: ExecutionContext)
  extends CookieBaker[UnAuthToken]
  with JWTCookieDataCodec {
  override val jwtConfiguration = JWTConfiguration()
  override val path: String = "/"
  override val COOKIE_NAME: String = CookieName
  override val emptyCookie: UnAuthToken = UnAuthToken.empty
  override val maxAge: Option[Int] = Some(365.days.toSeconds.toInt)

  override protected def serialize(cookie: UnAuthToken): Map[String, String] = Map(
    UserIdName -> cookie.user.name,
    SeriesName -> cookie.series.toString,
    TokenName -> cookie.token.toString
  )

  /** The API says we must return a token, even if deserialization fails, so we introduce the concept of an "empty" token
    * and filter it away in `readToken(RequestHeader)`.
    *
    * @param data token data
    * @return a token
    */
  override protected def deserialize(data: Map[String, String]): UnAuthToken = try {
    val maybeToken =
      for {
        u <- data get UserIdName
        s <- data get SeriesName
        t <- data get TokenName
      } yield UnAuthToken(Username(u), s.toLong, t.toLong)
    maybeToken getOrElse UnAuthToken.empty
  } catch {
    case _: NumberFormatException => UnAuthToken.empty
  }

  /**
    * @param req request
    * @return the browser's possibly stored token
    */
  def readToken(req: RequestHeader): Option[UnAuthToken] = {
    val cookie = req.cookies get COOKIE_NAME
    log debug s"Reading cookie: $cookie"
    val maybeEmptyToken = decodeFromCookie(cookie)
    if (!maybeEmptyToken.isEmpty) {
      log debug s"Read token: $maybeEmptyToken"
    }
    Option(maybeEmptyToken).filterNot(_.isEmpty)
  }

  /**
    * @return the authenticated user, along with an optional cookie to include
    */
  def authenticateFromCookie(request: RequestHeader): Future[Option[AuthedRequest]] =
    authenticateToken(request) map { maybeToken =>
      maybeToken.map(token => AuthedRequest(token.user, request, Some(cookify(token))))
    }

  /**
    * @return an authenticated token
    */
  def authenticateToken(req: RequestHeader): Future[Option[Token]] =
    authenticate(req).map(_.toOption)

  def authenticate(req: RequestHeader): Future[Either[AuthFailure, Token]] =
    readToken(req).map(t => cookieAuth(t, req)) getOrElse {
      log debug s"Found no token in request: ${req.cookies}"
      Future.successful(Left(MissingCookie(req)))
    }

  def cookify(token: Token) = encodeAsCookie(token.asUnAuth)

  def persistNewCookie(loggedInUser: Username): Future[Cookie] =
    createToken(loggedInUser).map(cookify)

  private def createToken(loggedInUser: Username): Future[Token] = {
    val token = Token(loggedInUser, Random.nextLong(), Random.nextLong())
    store.persist(token).map(_ => token)
  }

  private def cookieAuth(
    attempt: UnAuthToken,
    rh: RequestHeader
  ): Future[Either[AuthFailure, Token]] = {
    log debug s"Authenticating: $attempt"
    val user = attempt.user
    store.findToken(user, attempt.series).flatMap { maybeToken =>
      maybeToken.map { savedToken =>
        if (savedToken.token == attempt.token) {

          /**
            * I believe the intention is to ensure that a browser cannot reuse another browser's token.
            *
            * The token is replaced with a new one at each successful token authentication, while the series remains the
            * same; this updated cookie is then sent to the browser. The series acts as a browser identifier. So, if
            * there's a token mismatch, it suggests some other actor has authenticated using this browser's token, which is
            * suspect.
            */
          log info s"Cookie authentication succeeded. Updating token."
          for {
            _ <- store.remove(savedToken)
            newToken = Token(user, attempt.series, Random.nextLong())
            _ <- store.persist(newToken)
          } yield Right(newToken)
        } else {
          log warn s"The saved token did not match the one from the request. Refusing access."
          store.removeAll(user).map(_ => Left(InvalidCookie(rh)))
        }
      }.getOrElse {
        log debug s"Unable to authenticate token: $attempt"
        Future.successful(Left(InvalidCredentials(rh)))
      }
    }
  }
}
