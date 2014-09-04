package controllers

import java.io.FileNotFoundException
import java.nio.file.Path

import com.mle.play.controllers.{AuthRequest, BaseSecurity, FileUploadRequest, OneFileUploadRequest}
import com.mle.util.{Log, Utils}
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.io.BufferedSource

/**
 * @author mle
 */
trait SecureBase extends PimpContentController with BaseSecurity with Log {
  /**
   * Validates the supplied credentials.
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
  override def validateCredentials(username: String, password: String): Boolean = {
    import controllers.PimpAccountController._
    Utils.opt[BufferedSource, FileNotFoundException](io.Source.fromFile(passFile.toFile))
      .flatMap(_.getLines().toList.headOption)
      .fold(ifEmpty = username == defaultUser && password == defaultPass)(_ == hash(username, password))
  }

  def hash(username: String, password: String) =
    DigestUtils.md5Hex(username + ":" + password)

  /**
   * Authenticates, logs authenticated request, executes action, in that order.
   *
   * If authentication fails, logs auth fail message.
   */
  def AuthenticatedAndLogged(f: String => EssentialAction): EssentialAction =
    Authenticated(user => Logged(user, f))

  /**
   * Returns an action with the result specified in <code>onFail</code> if authentication fails.
   *
   * @param onFail result to return if authentication fails
   * @param f the action we want to do
   */
  def CustomFailingPimpAction(onFail: RequestHeader => Result)(f: String => Result) =
    Security.Authenticated(req => authenticate(req), req => onFail(req))(user => {
      Logged(user, user => Action(f(user)))
    })

  def PimpAction(f: AuthRequest[AnyContent] => Result) =
    PimpParsedAction(parse.anyContent)(f)

  def PimpAction(result: => Result) =
    PimpParsedAction(parse.anyContent)(_ => result)

  def OkPimpAction(f: AuthRequest[AnyContent] => Unit) =
    PimpAction(req => {
      f(req)
      Ok
    })

  def HeadPimpUploadAction(f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) =
    PimpUploadAction(req => req.files.headOption
      .map(firstFile => f(new OneFileUploadRequest[MultipartFormData[TemporaryFile]](firstFile, req.user, req)))
      .getOrElse(BadRequest))

  def PimpUploadAction(f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) =
    PimpParsedAction(parse.multipartFormData)(req => {
      val files: Seq[Path] = saveFiles(req)
      f(new FileUploadRequest(files, req.user, req))
    })

  def PimpParsedAction[T](parser: BodyParser[T] = parse.anyContent)(f: AuthRequest[T] => Result) =
    AuthenticatedAndLogged(user => Action(parser)(req => f(new AuthRequest(user, req))))
}