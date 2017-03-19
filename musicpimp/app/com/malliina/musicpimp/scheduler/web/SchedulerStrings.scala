package com.malliina.musicpimp.scheduler.web

trait SchedulerStrings extends AlarmStrings {
  val DESCRIPTION = "description"
  val Minutes = "minutes"
  val Hours = "hours"
  val Days = "days"
  val Enabled = "enabled"
  val AMOUNT = "amount"
  val UNIT = "unit"
  val On = "on"
  val OFF = "off"
  val TrackKey = "track"

  val PATH = "path"
  val PUSH_ADD = "push_add"
  val PUSH_REMOVE = "push_remove"
  val GCM_ADD = "gcm_add"
  val GCM_REMOVE = "gcm_remove"
  val ADM_ADD = "adm_add"
  val ADM_REMOVE = "adm_remove"
  val ApnsAdd = "apns_add"
  val ApnsRemove = "apns_remove"
  val URL = "url"
  val TAG = "tag"
}

object SchedulerStrings extends SchedulerStrings