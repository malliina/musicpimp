package com.malliina.play.models

import com.malliina.play.http.BaseAuthRequest
import com.malliina.values.Username
import play.api.mvc.RequestHeader

trait AuthInfo extends BaseAuthRequest[Username] {
  def user: Username

  def rh: RequestHeader
}
