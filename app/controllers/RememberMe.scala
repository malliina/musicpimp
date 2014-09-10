package controllers

import com.mle.musicpimp.auth.Token
import com.mle.play.controllers.AuthResult
import com.mle.util.Log
import play.api.mvc.{Cookie, CookieBaker, DiscardingCookie, RequestHeader}

import scala.util.Random

/**
 * Adapted from https://github.com/wsargent/play20-rememberme
 *
 * @author Michael
 */
sealed trait AuthFailure

case object CookieMissing extends AuthFailure

case object InvalidCookie extends AuthFailure

case object InvalidCredentials extends AuthFailure

case class UnAuthToken(user: String, series: Long, token: Long) {
  lazy val isEmpty = this == UnAuthToken.empty
}

object UnAuthToken {
  val empty = UnAuthToken("", 0, 0)
}

/**
 *
 */
trait RememberMe extends Log {
  def store: TokenStore

  /**
   * @return the authenticated user, along with an optional cookie to include
   */
  def authenticateFromCookie(req: RequestHeader): Option[AuthResult] =
    authenticateToken(req) map (token => AuthResult(token.user, Some(cookify(token))))

  /**
   *
   * @return an authenticated token
   */
  def authenticateToken(req: RequestHeader): Option[Token] = authenticate(req).right.toOption

  def authenticate(req: RequestHeader): Either[AuthFailure, Token] =
    RememberMe.readToken(req).map(cookieAuth) getOrElse {
      log debug s"Found no token in request: ${req.cookies}"
      Left(CookieMissing)
    }

  def cookify(token: Token) = RememberMe.encodeAsCookie(token.asUnAuth)

  def persistNewCookie(loggedInUser: String): Cookie = cookify(createToken(loggedInUser))

  private def createToken(loggedInUser: String): Token = {
    val token = Token(loggedInUser, Random.nextLong(), Random.nextLong())
    store persist token
    token
  }

  private def cookieAuth(attempt: UnAuthToken): Either[AuthFailure, Token] = {
    log debug s"Authenticating: $attempt"
    val user = attempt.user
    store.findToken(user, attempt.series).map(savedToken => {
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
        store remove savedToken
        val newToken = Token(user, attempt.series, Random.nextLong())
        store persist newToken
        Right(newToken)
      } else {
        log warn s"The saved token did not match the one from the request. Refusing access."
        store removeAll user
        Left(InvalidCookie)
      }
    }).getOrElse {
      log debug s"Unable to authenticate token: $attempt"
      Left(InvalidCredentials)
    }
  }
}

object RememberMe extends CookieBaker[UnAuthToken] with Log {
  val COOKIE_NAME = "REMEMBER_ME"
  val SERIES_NAME = "series"
  val USER_ID_NAME = "userId"
  val TOKEN_NAME = "token"
  val discardingCookie = DiscardingCookie(COOKIE_NAME)

  import scala.concurrent.duration.DurationInt

  override def maxAge: Option[Int] = Some(365.days.toSeconds.toInt)

  def readToken(req: RequestHeader): Option[UnAuthToken] = {
    val cookie = req.cookies get COOKIE_NAME
    log debug s"Reading cookie: $cookie"
    val tokenMaybeEmpty = decodeFromCookie(cookie)
    if (!tokenMaybeEmpty.isEmpty) {
      log debug s"Read token: $tokenMaybeEmpty"
    }
    Option(tokenMaybeEmpty).filterNot(_.isEmpty)
  }

  override val emptyCookie: UnAuthToken = UnAuthToken.empty

  override protected def serialize(cookie: UnAuthToken): Map[String, String] = Map(
    USER_ID_NAME -> cookie.user,
    SERIES_NAME -> cookie.series.toString,
    TOKEN_NAME -> cookie.token.toString
  )

  override protected def deserialize(data: Map[String, String]): UnAuthToken = try {
    val maybeToken =
      for {
        u <- data get USER_ID_NAME
        s <- data get SERIES_NAME
        t <- data get TOKEN_NAME
      } yield UnAuthToken(u, s.toLong, t.toLong)
    maybeToken getOrElse UnAuthToken.empty
  } catch {
    case nfe: NumberFormatException => UnAuthToken.empty
  }
}

