package controllers

import play.api.mvc._
import com.mle.util.{Utils, Log}
import com.mle.play.controllers.{OneFileUploadRequest, FileUploadRequest, AuthRequest, BaseSecurity}
import scala.io.BufferedSource
import java.io.{InputStream, FileNotFoundException}
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.{Files => PlayFiles}
import java.nio.file.Path
import play.api.libs.Files.TemporaryFile
import com.mle.play.streams.StreamParsers
import com.mle.audio.meta.StreamInfo
import com.mle.audio.javasound.JavaSoundPlayer
import scala.concurrent.Future

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
  def validateCredentials(username: String, password: String): Boolean = {
    import PimpAccountController._
    Utils.opt[BufferedSource, FileNotFoundException](io.Source.fromFile(passFile.toFile))
      .flatMap(_.getLines().toList.headOption)
      .map(_ == hash(username, password))
      .getOrElse(username == defaultUser && password == defaultPass)
  }

  def hash(username: String, password: String) =
    DigestUtils.md5Hex(username + ":" + password)

  /**
   * Authenticates, checks trial, logs authenticated request, executes action, in that order.
   *
   * If authentication fails, logs auth fail message; if the trial has expired,
   * logs trial expired message but not the authenticated request that never happened.
   */
  def AuthenticatedAndLogged(f: String => EssentialAction): EssentialAction =
    Authenticated(user => Logged(user, f))

  /**
   * Returns an action with the result specified in <code>onFail</code>
   * if authentication fails.
   *
   * @param onFail result to return if authentication fails
   * @param f the action we want to do
   */
  def CustomFailingPimpAction(onFail: RequestHeader => SimpleResult)(f: String => SimpleResult) =
    Security.Authenticated(req => authenticate(req), req => onFail(req))(user => {
      Logged(user, user => Action(f(user)))
    })

  def PimpAction(f: AuthRequest[AnyContent] => SimpleResult) =
    PimpParsedAction(parse.anyContent)(f)

  def PimpAction(result: => SimpleResult) =
    PimpParsedAction(parse.anyContent)(_ => result)

  def OkPimpAction(f: AuthRequest[AnyContent] => Unit) =
    PimpAction(req => {
      f(req)
      Ok
    })

  def HeadPimpUploadAction(f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult) =
    PimpUploadAction(req => req.files.headOption
      .map(firstFile => f(new OneFileUploadRequest[MultipartFormData[TemporaryFile]](firstFile, req.user, req)))
      .getOrElse(BadRequest))

  def PimpUploadAction(f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult) =
    PimpParsedAction(parse.multipartFormData)(req => {
      val files: Seq[Path] = saveFiles(req)
      f(new FileUploadRequest(files, req.user, req))
    })

  def PimpParsedAction[T](parser: BodyParser[T] = parse.anyContent)(f: AuthRequest[T] => SimpleResult) =
    AuthenticatedAndLogged(user => Action(parser)(req => f(new AuthRequest(user, req))))
}