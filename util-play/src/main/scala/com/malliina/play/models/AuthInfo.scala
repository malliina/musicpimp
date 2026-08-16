package com.malliina.play.models

import com.malliina.play.http.BaseAuthRequest
import com.malliina.values.Username
import org.http4s.Request

trait AuthInfo extends BaseAuthRequest[Username]:
  def user: Username
  def rh: Request[?]
