package com.malliina.play.http

import com.malliina.values.Username
import play.api.mvc.{AnyContent, Cookie, Request}

class FullRequest(user: Username, val request: Request[AnyContent], cookie: Option[Cookie])
  extends CookiedRequest[AnyContent, Username](user, request, cookie)
