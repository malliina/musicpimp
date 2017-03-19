package com.malliina.musicpimp.scheduler

import com.malliina.play.json.JsonEnum

object WeekDays extends JsonEnum[WeekDay] {
  override val all = WeekDay.EveryDay

  override def resolveName(w: WeekDay) = w.shortName
}
