package com.malliina.play.http

import com.malliina.play.models.AuthInfo
import com.malliina.values.Username
import org.http4s.Request

import java.nio.file.Path

trait BaseAuthRequest[U]:
  def user: U
  def rh: Request[?]

/** @tparam U
  *   type of authenticated user
  */
class CookiedRequest[U](val user: U, request: Request[?]) //, val cookie: Option[Cookie] = None)
  extends BaseAuthRequest[U]:
  override def rh: Request[?] = request

class FullRequest(user: Username, val request: Request[?]) //, cookie: Option[Cookie])
  extends CookiedRequest[Username](user, request)

class OneFileUploadRequest[A](val file: Path, user: String, request: Request[?])
  extends CookiedRequest(user, request)

class FileUploadRequest[A, U](val files: Seq[Path], user: U, request: Request[?])
  extends CookiedRequest(user, request)

class AuthRequest(val user: Username, val rh: Request[?]) extends AuthInfo

class AuthedRequest(user: Username, rh: Request[?]) //, val cookie: Option[Cookie] = None)
  extends AuthRequest(user, rh):

  def fillAny(completeRequest: Request[?]): FullRequest =
    new FullRequest(user, completeRequest) // , cookie)

  def fill[A](fullRequest: Request[?]): CookiedRequest[Username] =
    new CookiedRequest[Username](user, fullRequest)
