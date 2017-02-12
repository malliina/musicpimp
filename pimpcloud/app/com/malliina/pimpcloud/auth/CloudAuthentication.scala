package com.malliina.pimpcloud.auth

import com.malliina.pimpcloud.CloudCredentials
import controllers.pimpcloud.{PhoneConnection, ServerRequest}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait CloudAuthentication {
  def authServer(req: RequestHeader): Future[ServerRequest]

  def authPhone(req: RequestHeader): Future[PhoneConnection]

  def validate(creds: CloudCredentials): Future[PhoneConnection]
}
