package com.malliina.play.auth

import play.api.mvc.RequestHeader

sealed trait AuthFailure {
  def rh: RequestHeader
}

case class InvalidCredentials(rh: RequestHeader) extends AuthFailure

case class MissingCredentials(rh: RequestHeader) extends AuthFailure

case class InvalidCookie(rh: RequestHeader) extends AuthFailure

case class MissingCookie(rh: RequestHeader) extends AuthFailure
