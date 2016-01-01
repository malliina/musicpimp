package com.malliina.musicpimp.scheduler.web

/**
 *
 * @author mle
 */
trait SchedulerStrings {
  val DESCRIPTION = "description"
  val MINUTES = "minutes"
  val HOURS = "hours"
  val DAYS = "days"
  val ENABLED = "enabled"
  val AMOUNT = "amount"
  val UNIT = "unit"
  val ON = "on"
  val OFF = "off"
  val TRACK = "track"
  val TRACK_ID = "track_id"

  val CMD = "cmd"
  val DELETE = "delete"
  val SAVE = "save"
  val START = "start"
  val STOP = "stop"
  val ID = "id"
  val TASK_ID = "task_id"
  val AP = "ap"

  val PATH = "path"
  val PUSH_ADD = "push_add"
  val PUSH_REMOVE = "push_remove"
  val GCM_ADD = "gcm_add"
  val GCM_REMOVE = "gcm_remove"
  val ADM_ADD = "adm_add"
  val ADM_REMOVE = "adm_remove"
  val URL = "url"
  val TAG = "tag"
}

object SchedulerStrings extends SchedulerStrings