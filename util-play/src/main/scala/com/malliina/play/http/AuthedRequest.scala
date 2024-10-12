package com.malliina.play.http

import com.malliina.play.models.AuthRequest
import com.malliina.values.Username
import play.api.mvc.{AnyContent, Cookie, Request, RequestHeader}

class AuthedRequest(user: Username, rh: RequestHeader, val cookie: Option[Cookie] = None)
  extends AuthRequest(user, rh) {

  def fillAny(completeRequest: Request[AnyContent]): FullRequest =
    new FullRequest(user, completeRequest, cookie)

  def fill[A](fullRequest: Request[A]): CookiedRequest[A, Username] =
    new CookiedRequest[A, Username](user, fullRequest, cookie)
}

object AuthedRequest {
  def apply(user: Username, request: RequestHeader, cookie: Option[Cookie] = None) =
    new AuthedRequest(user, request, cookie)
}
