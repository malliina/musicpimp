package com.malliina.util

import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory

object Logging:
  val levels = Seq(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)
  val logger = LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]

  def level = Option(logger.getLevel) getOrElse Level.INFO

  def level_=(level: Level) = logger.setLevel(level)
