package com.malliina.exception

class ConfigException(msg: String, t: Throwable) extends GenericException(msg, t) {
  def this(msg: String) = this(msg, null)
}
