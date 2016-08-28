package controllers

import java.nio.file.Path

import akka.stream.Materializer
import com.malliina.musicpimp.models.User
import com.malliina.play.Authenticator
import com.malliina.play.auth.BasicCredentials
import com.malliina.play.concurrent.FutureOps2
import com.malliina.play.controllers._
import com.malliina.play.http._
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
    with BaseSecurity
    with Controller{

  override def authenticate(request: RequestHeader): Future[Option[AuthedRequest]] =
    super.authenticate(request).checkOrElse(_.nonEmpty, auth.authenticateFromCookie(request))

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
  def validateCredentials(username: User, password: String): Future[Boolean] =
    auth.authenticate(username, password)

  override def validateCredentials(creds: BasicCredentials): Future[Boolean] =
    validateCredentials(User(creds.username), creds.password)

  /** Authenticates, logs authenticated request, executes action, in that order.
    *
    * If authentication fails, logs auth fail message.
    */
  def authenticatedAndLogged(f: AuthedRequest => EssentialAction): EssentialAction =
    authenticated(user => logged(user, f))

  /** Returns an action with the result specified in <code>onFail</code> if authentication fails.
    *
    * @param onFail result to return if authentication fails
    * @param f      the action we want to do
    */
  def customFailingPimpAction(onFail: RequestHeader => Result)(f: AuthedRequest => Result) =
    authenticatedAsync(req => authenticate(req), onFail) { user =>
      logged(user, user => Action(req => maybeWithCookie(user, f(user))))
    }

  def okPimpAction(f: CookiedRequest[AnyContent, User] => Unit) =
    pimpAction { req =>
      f(req)
      Ok
    }

  def pimpAction(result: => Result): EssentialAction =
    pimpAction(_ => result)

  def pimpAction(f: CookiedRequest[AnyContent, User] => Result): EssentialAction =
    pimpParsedAction(parse.default)(req => f(req))

  def headPimpUploadAction(f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) =
    pimpUploadAction { req => req.files.headOption
      .map(firstFile => f(new OneFileUploadRequest[MultipartFormData[TemporaryFile]](firstFile, req.user.name, req)))
      .getOrElse(BadRequest)
    }

  def pimpUploadAction(f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile], User] => Result) =
    pimpParsedAction(parse.multipartFormData) { req =>
      val files: Seq[Path] = saveFiles(req)
      f(new FileUploadRequest(files, req.user, req))
    }

  def pimpParsedAction[T](parser: BodyParser[T])(f: CookiedRequest[T, User] => Result) =
    pimpParsedActionAsync(parser)(req => Future.successful(f(req)))

  def pimpActionAsync(f: CookiedRequest[AnyContent, User] => Future[Result]) =
    pimpParsedActionAsync(parse.default)(f)

  def pimpActionAsync2[R: Writeable](f: CookiedRequest[AnyContent, User] => Future[R]) =
    okAsyncAction(parse.default)(f)

  def okAsyncAction[T, R: Writeable](parser: BodyParser[T])(f: CookiedRequest[T, User] => Future[R]) =
    actionAsync(parser)(req => f(req).map(r => Ok(r)))

  def actionAsync[T](parser: BodyParser[T])(f: CookiedRequest[T, User] => Future[Result]) =
    pimpParsedActionAsync(parser)(req => f(req).recover(errorHandler))

  def pimpParsedActionAsync[T](parser: BodyParser[T])(f: CookiedRequest[T, User] => Future[Result]): EssentialAction =
    authenticatedAndLogged { auth =>
      Action.async(parser) { req =>
        val resultFuture = f(new CookiedRequest(User(auth.user.name), req, auth.cookie))
        resultFuture.map(r => maybeWithCookie(auth, r))
      }
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
  private def maybeWithCookie(auth: AuthedRequest, result: Result): Result = {
    auth.cookie.fold(result) { c =>
      log debug s"Sending updated cookie in response to user ${auth.user}..."
      result withCookies c withSession (Security.username -> auth.user.name)
    }
  }
}

object SecureBase {
  private val log = Logger(getClass)
}
