package com.malliina.pimpcloud.http4s

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.http.Errors
import com.malliina.http4s.BasicService.noCache
import com.malliina.musicpimp.auth.{Http4sAuth, Http4sAuthFailure, UserPayload}
import com.malliina.musicpimp.http4s.Responses
import com.malliina.pimpcloud.http4s.AuthProvider.Google
import com.malliina.pimpcloud.http4s.GoogleAuth.{LoginSession, log}
import com.malliina.play.http.FullUrls2
import com.malliina.util.AppLogger
import com.malliina.values.Literals.email
import com.malliina.values.{Email, Username}
import com.malliina.web.{AuthError, Callback, Code, GoogleAuthFlow, LoginHint, OAuthKeys, Start, Utils}
import io.circe.Codec
import org.http4s.{Request, Response, Uri}

object GoogleAuth:
  private val log = AppLogger(getClass)

  case class CookieConf(
    user: String,
    session: String,
    returnUri: String,
    lastId: String,
    provider: String,
    prompt: String
  )

  val googleCookies = CookieConf(
    "google-user",
    "google-session",
    "google-return-uri",
    "google-last-id",
    "provider",
    "google-prompt"
  )

  case class LoginSession(state: String, nonce: Option[String]) derives Codec.AsObject

class GoogleAuth[F[_]: Async](
  val google: GoogleAuthFlow[F],
  reverse: GoogleUris,
  cookieNames: GoogleAuth.CookieConf,
  auth: Http4sAuth[F]
) extends Responses[F]
  with PimpExt:
  val F = Async[F]

  val authorizedEmail: Email = email"malliina123@gmail.com"

  def authed(req: Request[F])(content: UserPayload => F[Response[F]]): F[Response[F]] =
    authenticate(req)
      .map: user =>
        val isAuthorized = Email.build(user.username.name).contains(authorizedEmail)
        if isAuthorized then content(user)
        else unauthorizedNoCacheWithErrors(Errors.single("Unauthorized."))
      .handleLeft: failure =>
        log.info(s"Unauthorized $failure")
        seeOther(reverse.oauth)

  def authenticate(req: Request[F]): Either[Http4sAuthFailure, UserPayload] =
    auth.read[UserPayload](cookieNames.user, req)

  def googleCallback(req: Request[F]) =
    handleCallback(
      req,
      Google,
      cb => google.validateCallback(cb).map(e => e.flatMap(google.parse))
    )

  private def handleCallback(
    req: Request[F],
    provider: AuthProvider,
    validate: Callback => F[Either[AuthError, Email]]
  ): F[Response[F]] =
    val params = req.uri.query.params
    auth
      .read[LoginSession](cookieNames.session, req)
      .map: session =>
        val cb = Callback(
          params.get(OAuthKeys.State),
          Option(session.state),
          params.get(OAuthKeys.CodeKey).flatMap(Code.build(_).toOption),
          session.nonce,
          FullUrls2.hostOnly2(req) / reverse.callback.renderString
        )
        validate(cb).flatMap: e =>
          e.fold(
            err => unauthorizedNoCacheWithErrors(Errors(err.message)),
            email => userResult(email, provider, req)
          )
      .handleLeft: err =>
        badRequest("Invalid state.")

  private def userResult(
    email: Email,
    provider: AuthProvider,
    req: Request[F]
  ): F[Response[F]] =
    val returnUri: Uri = req.cookies
      .find(_.name == cookieNames.returnUri)
      .flatMap(c => Uri.fromString(c.content).toOption)
      .getOrElse(reverse.root)
    seeOther(returnUri).map: res =>
      auth.withJwt(
        cookieNames.user,
        UserPayload(Username.fromEmail(email)),
        FullUrls2.isSecure(req),
        res
      )

  def startHinted(
    provider: AuthProvider,
    validator: LoginHint[F],
    req: Request[F]
  ): F[Response[F]] = F
    .delay:
      val redirectUrl = FullUrls2.hostOnly2(req) / reverse.callback.renderString
      val lastIdCookie = req.cookies.find(_.name == cookieNames.lastId)
      val promptValue = req.cookies
        .find(_.name == cookieNames.prompt)
        .map(_.content)
        .orElse(Option(AuthProvider.SelectAccount).filter(_ => lastIdCookie.isEmpty))
      val extra = promptValue.map(c => Map(AuthProvider.PromptKey -> c)).getOrElse(Map.empty)
      val maybeEmail = lastIdCookie.map(_.content).filter(_ => extra.isEmpty)
      maybeEmail.foreach: hint =>
        log.info(s"Starting OAuth flow with $provider using login hint '$hint'...")
      promptValue.foreach: prompt =>
        log.info(s"Starting OAuth flow with $provider using prompt '$prompt'...")
      (redirectUrl, maybeEmail, extra)
    .flatMap:
      case (redirectUrl, maybeEmail, extra) =>
        validator
          .startHinted(redirectUrl, maybeEmail, extra)
          .flatMap: s =>
            startLoginFlow(s, req)

  private def startLoginFlow(s: Start, req: Request[F]): F[Response[F]] = F
    .delay:
      val state = Utils.randomString()
      val encodedParams = (s.params ++ Map(OAuthKeys.State -> state)).map: (k, v) =>
        k -> Utils.urlEncode(v)
      val url = s.authorizationEndpoint.append(s"?${Utils.stringify(encodedParams)}")
      log.info(s"Redirecting to '$url' with state '$state'...")
      (url, LoginSession(state, s.nonce))
    .flatMap: (url, session) =>
      seeOther(Uri.unsafeFromString(url.url)).map: res =>
        auth.withJwt(cookieNames.session, session, FullUrls2.isSecure(req), res).putHeaders(noCache)
