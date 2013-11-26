package com.mle.musicpimp.exception

/**
 * @author Michael
 */
class PimpException(msg: String, t: Throwable) extends Exception(msg, t) {
  def this(msg: String) = this(msg, null)
}
