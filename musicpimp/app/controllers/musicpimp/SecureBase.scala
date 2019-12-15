package controllers.musicpimp

import java.nio.file.Path

import com.malliina.musicpimp.http.PimpUploads
import com.malliina.play.auth.{Auth, AuthFailure}
import com.malliina.play.controllers._
import com.malliina.play.http._
import com.malliina.values.Username
import controllers.musicpimp.SecureBase.log
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.Files.TemporaryFile
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.concurrent.Future

class SecureBase(auth: AuthDeps)
  extends BaseSecurity[AuthedRequest](auth.comps.actionBuilder, auth.auth, auth.mat) {
  val comps = auth.comps
  val Action = comps.actionBuilder
  val parsers = comps.parsers
  val defaultParser = parsers.default

  val uploads = PimpUploads

  // temp hack
  val Accepted = Results.Accepted
  val BadRequest = Results.BadRequest
  val NotFound = Results.NotFound
  val Ok = Results.Ok
  val Unauthorized = Results.Unauthorized

  def Redirect(call: Call) = Results.Redirect(call)

  /** Returns an action with the result specified in <code>onFail</code> if authentication fails.
    *
    * @param onFail result to return if authentication fails
    * @param f      the action we want to do
    */
  def customFailingPimpAction(onFail: AuthFailure => Result)(f: AuthedRequest => Future[Result]) =
    authenticatedAsync(req => authenticate(req), onFail) { user =>
      logged(user, user => Action.async(_ => f(user).map(r => maybeWithCookie(user, r))))
    }

  def okPimpAction(f: CookiedRequest[AnyContent, Username] => Unit) =
    pimpAction { req =>
      f(req)
      Ok
    }

  def pimpAction(result: => Result): EssentialAction =
    pimpAction(_ => result)

  def pimpAction(f: CookiedRequest[AnyContent, Username] => Result): EssentialAction =
    pimpParsedAction(defaultParser)(req => f(req))

  def headPimpUploadAction(
    f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Future[Result]
  ) =
    pimpUploadAction { req =>
      req.files.headOption
        .map(firstFile =>
          f(
            new OneFileUploadRequest[MultipartFormData[TemporaryFile]](
              firstFile,
              req.user.name,
              req
            )
          )
        )
        .getOrElse(fut(badRequest(s"File missing")))
    }

  def pimpUploadAction(
    f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile], Username] => Future[Result]
  ) =
    pimpParsedActionAsync(parsers.multipartFormData) { req =>
      val files: Seq[Path] = uploads.save(req)
      f(new FileUploadRequest(files, req.user, req))
    }

  def pimpParsedAction[T](parser: BodyParser[T])(f: CookiedRequest[T, Username] => Result) =
    pimpParsedActionAsync(parser)(req => fut(f(req)))

  def pimpActionAsync(f: CookiedRequest[AnyContent, Username] => Future[Result]) =
    pimpParsedActionAsync(defaultParser)(f)

  def pimpActionAsync2[R: Writeable](f: CookiedRequest[AnyContent, Username] => Future[R]) =
    okAsyncAction(defaultParser)(f)

  def okAsyncAction[T, R: Writeable](
    parser: BodyParser[T]
  )(f: CookiedRequest[T, Username] => Future[R]) =
    actionAsync(parser)(req => f(req).map(r => Ok(r)))

  def actionAsync[T](parser: BodyParser[T])(f: CookiedRequest[T, Username] => Future[Result]) =
    pimpParsedActionAsync(parser)(req => f(req).recover(errorHandler))

  def pimpParsedActionAsync[T](
    parser: BodyParser[T]
  )(f: CookiedRequest[T, Username] => Future[Result]): EssentialAction =
    authenticatedLogged { (auth: AuthedRequest) =>
      comps.actionBuilder.async(parser) { req =>
        val resultFuture = f(new CookiedRequest(auth.user, req, auth.cookie))
        resultFuture.map(r => maybeWithCookie(auth, r))
      }
    }

  def errorHandler: PartialFunction[Throwable, Result] = {
    case _ => serverErrorGeneric
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
      result withCookies c withSession (Auth.DefaultSessionKey -> auth.user.name)
    }
  }
}

object SecureBase {
  private val log = Logger(getClass)
}
