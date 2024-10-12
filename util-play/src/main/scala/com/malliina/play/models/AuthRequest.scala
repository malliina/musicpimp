package com.malliina.play.models

import com.malliina.values.Username
import play.api.mvc.RequestHeader

class AuthRequest(val user: Username, val rh: RequestHeader) extends AuthInfo
