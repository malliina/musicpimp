package com.malliina.musicpimp.scheduler.json

import com.malliina.musicpimp.json.CrossFormats.{cmd, singleCmd}
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.adm.ADMDevice
import com.malliina.musicpimp.messaging.apns.APNSDevice
import com.malliina.musicpimp.messaging.gcm.GCMDevice
import com.malliina.musicpimp.models.{Delete, Start, Stop}
import com.malliina.musicpimp.scheduler.ClockPlayback
import com.malliina.push.adm.ADMToken
import com.malliina.push.apns.APNSToken
import com.malliina.push.gcm.GCMToken
import com.malliina.push.mpns.PushUrl
import play.api.libs.json._

sealed trait AlarmCommand

case class SaveCmd(ap: ClockPlayback) extends AlarmCommand

object SaveCmd {
  val Key = "save"
  implicit val json = cmd(Key, Json.format[SaveCmd])
}

case class DeleteCmd(id: String) extends AlarmCommand

object DeleteCmd {
  implicit val json = cmd(Delete.Key, Json.format[DeleteCmd])
}

case class StartCmd(id: String) extends AlarmCommand

object StartCmd {
  implicit val json = cmd(Start.Key, Json.format[StartCmd])
}

case class AddWindowsDevice(pushUrl: PushUrl) extends AlarmCommand

object AddWindowsDevice {
  val Key = "push_add"
  implicit val json = cmd(Key, Json.format[AddWindowsDevice])
}

case class RemoveWindowsDevice(url: String) extends AlarmCommand

object RemoveWindowsDevice {
  val Key = "push_remove"
  implicit val json = cmd(Key, Json.format[RemoveWindowsDevice])
}

case class RemovePushTag(tag: ServerTag) extends AlarmCommand

object RemovePushTag {
  val Key = "push_remove"
  implicit val json = cmd(Key, Json.format[RemovePushTag])
}

case class AddGoogleDevice(id: GCMToken, tag: ServerTag) extends AlarmCommand {
  def dest = GCMDevice(id, tag)
}

object AddGoogleDevice {
  val Key = "gcm_add"
  implicit val json = cmd(Key, Json.format[AddGoogleDevice])
}

case class RemoveGoogleDevice(id: ServerTag) extends AlarmCommand

object RemoveGoogleDevice {
  val Key = "gcm_remove"
  implicit val json = cmd(Key, Json.format[RemoveGoogleDevice])
}

case class AddAmazonDevice(id: ADMToken, tag: ServerTag) extends AlarmCommand {
  def dest = ADMDevice(id, tag)
}

object AddAmazonDevice {
  val Key = "adm_add"
  implicit val json = cmd(Key, Json.format[AddAmazonDevice])
}

case class RemoveAmazonDevice(id: ServerTag) extends AlarmCommand

object RemoveAmazonDevice {
  val Key = "adm_remove"
  implicit val json = cmd(Key, Json.format[RemoveAmazonDevice])
}

case class AddApnsDevice(id: APNSToken, tag: ServerTag) extends AlarmCommand {
  def dest = APNSDevice(id, tag)
}

object AddApnsDevice {
  // I think this JSON format violates the API doc, thus registrations don't work
  val Key = "apns_add"
  implicit val json = cmd(Key, Json.format[AddApnsDevice])
}

case class RemoveApnsDevice(id: ServerTag) extends AlarmCommand

object RemoveApnsDevice {
  val Key = "apns_remove"
  implicit val json = cmd(Key, Json.format[RemoveApnsDevice])
}

case object StopPlayback extends AlarmCommand {
  implicit val json = singleCmd(Stop.Key, StopPlayback)
}

object AlarmCommand {
  implicit val reader = Reads[AlarmCommand] { json =>
    DeleteCmd.json.reads(json)
      .orElse(SaveCmd.json.reads(json))
      .orElse(StartCmd.json.reads(json))
      .orElse(StopPlayback.json.reads(json))
      .orElse(AddWindowsDevice.json.reads(json))
      .orElse(RemovePushTag.json.reads(json))
      .orElse(RemoveWindowsDevice.json.reads(json))
      .orElse(AddGoogleDevice.json.reads(json))
      .orElse(RemoveGoogleDevice.json.reads(json))
      .orElse(AddAmazonDevice.json.reads(json))
      .orElse(RemoveAmazonDevice.json.reads(json))
      .orElse(AddApnsDevice.json.reads(json))
      .orElse(RemoveApnsDevice.json.reads(json))
  }
}
