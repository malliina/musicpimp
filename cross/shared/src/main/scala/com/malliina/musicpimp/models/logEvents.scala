package com.malliina.musicpimp.models

import io.circe.Codec

case class FrontLogEvent(message: String, module: String, level: String) derives Codec.AsObject

object FrontLogEvent:
  val Info = "info"
  val Error = "error"

  def simple(message: String, module: String): FrontLogEvent =
    FrontLogEvent(message, module, Info)

  def error(message: String, module: String): FrontLogEvent =
    FrontLogEvent(message, module, Error)

case class FrontLogEvents(events: Seq[FrontLogEvent]) derives Codec.AsObject

object FrontLogEvents:
  def single(message: String, module: String) =
    FrontLogEvents(Seq(FrontLogEvent.simple(message, module)))

case class JVMLogEntry(
  level: String,
  message: String,
  loggerName: String,
  threadName: String,
  timeFormatted: String,
  stackTrace: Option[String] = None
) derives Codec.AsObject
