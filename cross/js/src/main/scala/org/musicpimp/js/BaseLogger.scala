package org.musicpimp.js

trait BaseLogger:
  def info(message: String): Unit
  def error(t: Throwable): Unit

object BaseLogger:
  val noop: BaseLogger = new BaseLogger:
    override def info(message: String): Unit = ()
    override def error(t: Throwable): Unit = ()

  val printer: BaseLogger = new BaseLogger:
    override def info(message: String): Unit = println(message)
    override def error(t: Throwable): Unit = println(s"Error: $t")
