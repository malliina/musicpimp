package com.malliina.musicpimp.scheduler

import play.api.libs.json.Json

case class Conf(clocks: Seq[ClockPlayback])

object Conf {
  val empty = Conf(Seq.empty)
  implicit val json = Json.format[Conf]
}