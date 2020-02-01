package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.concurrent.Execution.cached
import com.malliina.http.OkClient
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.play.auth.{
  AuthConf,
  AuthFailure,
  Authenticator,
  BasicAuthHandler,
  GoogleCodeValidator,
  OAuthConf,
  PermissionError,
  UserAuthenticator
}
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import com.malliina.play.http.AuthedRequest
import com.malliina.values.{Email, ErrorMessage}
import play.api.Logger
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc._

class ProdAuth(ctrl: OAuthCtrl) extends PimpAuth {
  override def logged(action: EssentialAction) =
    ctrl.logged(action)

  override def authenticate(request: RequestHeader) =
    ctrl.authenticate(request)

  override def authAction(f: AuthedRequest => Result) =
    ctrl.authAction(f)
}

trait PimpAuth extends Authenticator[AuthedRequest] {
  def logged(action: EssentialAction): EssentialAction

  def authAction(f: AuthedRequest => Result): EssentialAction

  def navigate[C: Writeable](f: AuthedRequest => C): EssentialAction =
    authAction(req => Ok(f(req)))
}

class OAuthCtrl(val oauth: AdminOAuth, mat: Materializer)
  extends BaseSecurity(oauth.actions, OAuthCtrl.bundle(oauth), mat)

object OAuthCtrl {
  def bundle(oauth: AdminOAuth) = new AuthBundle[AuthedRequest] {
    override def authenticator: Authenticator[AuthedRequest] =
      UserAuthenticator
        .session(oauth.sessionKey)
        .transform((req, user) => Right(AuthedRequest(user, req)))

    override def onUnauthorized(failure: AuthFailure): Result =
      Results.Redirect(routes.AdminOAuth.googleStart())
  }
}

class AdminOAuth(val actions: ActionBuilder[Request, AnyContent], creds: GoogleOAuthCredentials) {
  private val log = Logger(getClass)

  val sessionKey = "cloudUser"
  val lastIdKey = "cloudLastId"
  val authorizedEmail = Email("malliina123@gmail.com")
  val handler = new BasicAuthHandler(
    routes.Logs.index(),
    lastIdKey = lastIdKey,
    email =>
      if (email == authorizedEmail) Right(email)
      else Left(PermissionError(ErrorMessage(s"Unauthorized: '$email'."))),
    sessionKey = sessionKey,
    lastIdMaxAge = Option(BasicAuthHandler.DefaultMaxAge),
    returnUriKey = BasicAuthHandler.DefaultReturnUriKey
  )
  val conf = AuthConf(creds.clientId, creds.clientSecret)
  val oauthConf = OAuthConf(routes.AdminOAuth.googleCallback(), handler, conf, OkClient.default)
  val validator = GoogleCodeValidator(oauthConf)

  def googleStart = actions.async { req =>
    val lastId = req.cookies.get(handler.lastIdKey).map(_.value)
    val described = lastId.fold("without login hint")(h => s"with login hint '$h'")
    log.info(s"Starting OAuth flow $described.")
    validator.startHinted(req, lastId)
  }

  def googleCallback = actions.async { req =>
    validator.validateCallback(req)
  }
}
