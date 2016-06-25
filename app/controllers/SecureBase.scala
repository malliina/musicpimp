package controllers

import java.nio.file.Path

import akka.stream.Materializer
import com.malliina.musicpimp.models.User
import com.malliina.play.Authenticator
import com.malliina.play.auth.BasicCredentials
import com.malliina.play.concurrent.FutureOps2
import com.malliina.play.controllers._
import com.malliina.play.http.{AuthRequest, AuthResult, FileUploadRequest, OneFileUploadRequest}
import controllers.SecureBase.log
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.concurrent.Future

class SecureBase(auth: Authenticator, val mat: Materializer)
  extends PimpContentController
    with BaseSecurity {

  override def authenticate(implicit request: RequestHeader): Future[Option[AuthResult]] =
    super.authenticate.checkOrElse(_.nonEmpty, auth.authenticateFromCookie(request))

  /** Validates the supplied credentials.
    *
    * Hashes the supplied credentials and compares the hash with the first line of
    * the password file. If they equal, validation succeeds, otherwise it fails.
    *
    * If the password file doesn't exist, it means no password has ever been set thus
    * the credentials are compared against the default credentials.
    *
    * @param username the supplied username
    * @param password the supplied password
    * @return true if the credentials are valid, false otherwise
    */
  def validateCredentials(username: String, password: String): Future[Boolean] =
    auth.authenticate(User(username), password)

  override def validateCredentials(creds: BasicCredentials): Future[Boolean] =
    validateCredentials(creds.username, creds.password)

  /** Authenticates, logs authenticated request, executes action, in that order.
    *
    * If authentication fails, logs auth fail message.
    */
  def AuthenticatedAndLogged(f: AuthResult => EssentialAction): EssentialAction =
    Authenticated(user => Logged(user, f))

  /** Returns an action with the result specified in <code>onFail</code> if authentication fails.
    *
    * @param onFail result to return if authentication fails
    * @param f      the action we want to do
    */
  def customFailingPimpAction(onFail: RequestHeader => Result)(f: (RequestHeader, AuthResult) => Result) = {
    authenticatedAsync(req => authenticate(req), onFail) { user =>
      Logged(user, user => Action(req => maybeWithCookie(user, f(req, user))))
    }
  }

  def okPimpAction(f: AuthRequest[AnyContent] => Unit) =
    pimpAction { req =>
      f(req)
      Ok
    }

  def pimpAction(result: => Result): EssentialAction =
    pimpAction(_ => result)

  def pimpAction(f: AuthRequest[AnyContent] => Result): EssentialAction =
    pimpParsedAction(parse.default)(req => f(req))

  def headPimpUploadAction(f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) = {
    pimpUploadAction(req => req.files.headOption
      .map(firstFile => f(new OneFileUploadRequest[MultipartFormData[TemporaryFile]](firstFile, req.user, req)))
      .getOrElse(BadRequest))
  }

  def pimpUploadAction(f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) = {
    pimpParsedAction(parse.multipartFormData)(req => {
      val files: Seq[Path] = saveFiles(req)
      f(new FileUploadRequest(files, req.user, req))
    })
  }

  def pimpParsedAction[T](parser: BodyParser[T])(f: AuthRequest[T] => Result) =
    pimpParsedActionAsync(parser)(req => Future.successful(f(req)))

  def pimpActionAsync(f: AuthRequest[AnyContent] => Future[Result]) =
    pimpParsedActionAsync(parse.default)(f)

  def pimpActionAsync2[R: Writeable](f: AuthRequest[AnyContent] => Future[R]) =
    okAsyncAction(parse.default)(f)

  def okAsyncAction[T, R: Writeable](parser: BodyParser[T])(f: AuthRequest[T] => Future[R]) =
    actionAsync(parser)(req => f(req).map(r => Ok(r)))

  def actionAsync[T](parser: BodyParser[T])(f: AuthRequest[T] => Future[Result]) =
    pimpParsedActionAsync(parser)(req => f(req).recover(errorHandler))

  def pimpParsedActionAsync[T](parser: BodyParser[T])(f: AuthRequest[T] => Future[Result]): EssentialAction = {
    AuthenticatedAndLogged(auth => {
      Action.async(parser)(req => {
        val resultFuture = f(new AuthRequest(auth.user, req, auth.cookie))
        resultFuture.map(r => maybeWithCookie(auth, r))
      })
    })
  }

  def errorHandler: PartialFunction[Throwable, Result] = {
    case t => InternalServerError
  }

  /** Due to the "remember me" functionality, browser cookies are updated after a successful cookie-based authentication.
    * To achieve that, we remember the result of the authentication and then update any cookie, if necessary, in the
    * response to the request.
    *
    * @param auth   authentication output
    * @param result response
    * @return response, with possibly updated cookies
    */
  private def maybeWithCookie(auth: AuthResult, result: Result): Result = {
    auth.cookie.fold(result)(c => {
      log debug s"Sending updated cookie in response..."
      result withCookies c withSession (Security.username -> auth.user)
    })
  }
}

object SecureBase {
  private val log = Logger(getClass)
}
