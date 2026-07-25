package com.malliina.musicpimp.exception

class UnauthorizedException(friendlyMessage: String, t: Option[Throwable] = None)
  extends PimpException(friendlyMessage, t)
