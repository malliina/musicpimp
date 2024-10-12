package com.malliina.web

import cats.effect.IO
import com.malliina.http.FullUrl
import com.malliina.util.AppLogger
import com.malliina.values.{Email, ErrorMessage}
import com.malliina.web.OAuthKeys._

import scala.concurrent.Future

object EmailAuthFlow {
  def apply(conf: AuthCodeConf) = new EmailAuthFlow(conf)
}

class EmailAuthFlow(conf: AuthCodeConf) extends StandardAuthFlow[Email](conf) {
  override def parse(v: Verified): Either[JWTError, Email] =
    v.readString(EmailKey).map(Email.apply)
}

trait AuthFlow[F[_], U] extends FlowStart[F] with CallbackValidator[U]

trait FlowStart[F[_]] {
  def start(redirectUrl: FullUrl, extraParams: Map[String, String]): F[Start]

  def extraRedirParams(redirectUrl: FullUrl): Map[String, String] = Map.empty

  protected def commonAuthParams(
    authScope: String,
    redirectUrl: FullUrl,
    clientId: ClientId
  ): Map[String, String] =
    Map(
      RedirectUri -> redirectUrl.url,
      ClientIdKey -> clientId.value,
      Scope -> authScope
    )
}

trait StaticFlowStart extends FlowStart[IO] {
  def conf: StaticConf

  override def start(redirectUrl: FullUrl, extraParams: Map[String, String]): IO[Start] = {
    val params =
      commonAuthParams(conf.scope, redirectUrl, conf.authConf.clientId) ++ extraRedirParams(
        redirectUrl
      ) ++ extraParams
    IO.pure(Start(conf.authorizationEndpoint, params, None))
  }
}

trait LoginHint[F[_]] { self: FlowStart[F] =>
  def startHinted(
    redirectUrl: FullUrl,
    loginHint: Option[String],
    extraParams: Map[String, String]
  ): F[Start] =
    self.start(
      redirectUrl,
      extraParams ++ loginHint.map(lh => Map(LoginHint -> lh)).getOrElse(Map.empty)
    )
}

abstract class StandardAuthFlow[V](conf: AuthCodeConf)
  extends DiscoveringAuthFlow[V](conf)
  with LoginHint[IO]

object CallbackValidator {
  private val log = AppLogger(getClass)
}

trait CallbackValidator[U] {
  import CallbackValidator.log

  /** Returns either a successfully validated user object or an AuthError if validation fails.
    *
    * The returned Future fails with a ResponseException if any network request fails.
    *
    * @param code auth code
    * @param redirectUrl redir url
    * @return a user object or a failure
    */
  def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): IO[Either[AuthError, U]]

  def validateCallback(cb: Callback): IO[Either[AuthError, U]] = {
    val isStateOk = cb.requestState.exists(rs => cb.sessionState.contains(rs))
    if (isStateOk) {
      cb.codeQuery
        .map { code => validate(Code(code), cb.redirectUrl, cb.requestNonce) }
        .getOrElse {
          log.error(s"Authentication failed, code missing.")
          IO.pure(Left(OAuthError(ErrorMessage("Code missing."))))
        }
    } else {
      val detailed = (cb.requestState, cb.sessionState) match {
        case (Some(rs), Some(ss)) => s"Got '$rs', expected '$ss'."
        case (Some(rs), None)     => s"Got '$rs', but found nothing to compare to."
        case (None, Some(ss))     => s"No state in request, expected '$ss'."
        case _                    => "No state in request and nothing to compare to either."
      }
      log.error(s"Authentication failed, state mismatch. $detailed")
      IO.pure(Left(OAuthError(ErrorMessage("State mismatch."))))
    }
  }

  /** Not encoded.
    */
  protected def validationParams(
    code: Code,
    redirectUrl: FullUrl,
    conf: AuthConf
  ): Map[String, String] = Map(
    ClientIdKey -> conf.clientId.value,
    ClientSecretKey -> conf.clientSecret.value,
    RedirectUri -> redirectUrl.url,
    CodeKey -> code.code
  )
}
