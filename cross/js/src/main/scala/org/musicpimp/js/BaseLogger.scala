package org.musicpimp.js

trait BaseLogger {
  def info(message: String): Unit

  def error(t: Throwable): Unit
}

object BaseLogger {
  val noop: BaseLogger = new BaseLogger {
    override def error(t: Throwable): Unit = ()

    override def info(message: String): Unit = ()
  }
}
