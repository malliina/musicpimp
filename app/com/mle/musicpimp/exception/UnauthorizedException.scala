package com.mle.musicpimp.exception

/**
 * @author mle
 */
class UnauthorizedException(friendlyMessage: String, t: Option[Throwable] = None)
  extends PimpException(friendlyMessage, t)
