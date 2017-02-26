package com.malliina.it

import org.slf4j.{Logger, LoggerFactory}

object Logging {
  // ripped from Play's Logger.scala
  def apply[T](clazz: Class[T]): Logger =
    LoggerFactory.getLogger(clazz.getName.stripSuffix("$"))
}
