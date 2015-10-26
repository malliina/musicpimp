package com.mle.musicpimp.exception

/**
 * @author Michael
 */
class PimpException(val friendlyMessage: String, t: Option[Throwable])
  extends Exception(friendlyMessage, t.orNull)
