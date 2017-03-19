package com.malliina.pimpcloud.auth

import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.{AuthFailure, Authenticator}
import controllers.pimpcloud.ServerRequest
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait CloudAuthentication {
  type PhoneAuthResult = Future[Either[AuthFailure, PhoneConnection]]

  def authServer(req: RequestHeader): Future[Either[AuthFailure, ServerRequest]]

  def authPhone(req: RequestHeader): PhoneAuthResult

  def authWebClient(creds: CloudCredentials): PhoneAuthResult

  def phone: Authenticator[PhoneConnection]

  def server: Authenticator[ServerRequest]

  //def webClient: Authenticator[PhoneConnection]
}
