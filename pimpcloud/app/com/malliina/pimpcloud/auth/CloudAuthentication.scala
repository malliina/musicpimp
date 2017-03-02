package com.malliina.pimpcloud.auth

import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.AuthFailure
import controllers.pimpcloud.ServerRequest
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait CloudAuthentication {
  type PhoneAuthResult = Future[Either[AuthFailure, PhoneConnection]]

  def authServer(req: RequestHeader): Future[ServerRequest]

  def authPhone(req: RequestHeader): PhoneAuthResult

  def validate(creds: CloudCredentials): PhoneAuthResult
}
