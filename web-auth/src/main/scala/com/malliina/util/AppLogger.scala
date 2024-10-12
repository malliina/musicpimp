package com.malliina.util

import org.slf4j.{Logger, LoggerFactory}

object AppLogger {
  def apply(cls: Class[?]): Logger = {
    val name = cls.getName.reverse.dropWhile(_ == '$').reverse
    LoggerFactory.getLogger(name)
  }
}
