package com.malliina.play.http

import play.api.mvc.RequestHeader

trait BaseAuthRequest[U] {
  def user: U

  def rh: RequestHeader
}
