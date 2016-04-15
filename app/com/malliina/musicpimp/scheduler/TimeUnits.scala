package com.malliina.musicpimp.scheduler

import com.malliina.play.json.JsonEnum


sealed abstract class TimeUnit(val name: String) {
  override def toString = name
}

case object Minutes extends TimeUnit("minutes")

case object Hours extends TimeUnit("hours")

case object Days extends TimeUnit("days")

object TimeUnit extends JsonEnum[TimeUnit] {
  val all = Seq(Minutes, Hours, Days)

  override def resolveName(item: TimeUnit): String =
    item.name
}
