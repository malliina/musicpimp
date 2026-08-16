package com.malliina.pimpcloud.auth

import com.malliina.musicpimp.auth.{Authenticator, Http4sAuthFailure}
import com.malliina.pimpcloud.http4s.ServerRequest
import com.malliina.pimpcloud.ws.PhoneConnection
import org.http4s.Request

trait CloudAuthentication[F[_]]:
  type PhoneAuthResult = F[Either[Http4sAuthFailure, PhoneConnection[F]]]

  def authServer(req: Request[F]): F[Either[Http4sAuthFailure, ServerRequest[F]]]

  def authPhone(req: Request[F]): PhoneAuthResult

  def authWebClient(creds: CloudCredentials): PhoneAuthResult

  def phone: Authenticator[F, PhoneConnection[F]]

  def server: Authenticator[F, ServerRequest[F]]
