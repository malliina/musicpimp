package com.malliina.musicpimp.scheduler

sealed abstract class WeekDay(val shortName: String, val longName: String)

case object Monday extends WeekDay("mon", "Monday")
case object Tuesday extends WeekDay("tue", "Tuesday")
case object Wednesday extends WeekDay("wed", "Wednesday")
case object Thursday extends WeekDay("thu", "Thursday")
case object Friday extends WeekDay("fri", "Friday")
case object Saturday extends WeekDay("sat", "Saturday")
case object Sunday extends WeekDay("sun", "Sunday")

object WeekDay {
  def withShortName(name: String): Option[WeekDay] =
    EveryDay.find(_.shortName.toLowerCase == name.toLowerCase)

  val EveryDay = Seq(Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
  val EveryDaySet = EveryDay.toSet
  val Weekend = Seq(Saturday, Sunday)
  val WeekendSet = Weekend.toSet
  val WorkDays = EveryDay diff Weekend
}
