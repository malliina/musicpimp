package com.malliina.play.http

import com.malliina.values.Username
import org.http4s.Request

trait AuthInfo extends BaseAuthRequest[Username]:
  def user: Username
  def rh: Request[?]
