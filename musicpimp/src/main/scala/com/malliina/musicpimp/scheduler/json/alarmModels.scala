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
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder}

sealed trait AlarmCommand

case class SaveCmd(ap: ClockPlayback) extends AlarmCommand

object SaveCmd:
  val Key = "save"
  given json: Codec[SaveCmd] = cmd(Key, deriveCodec[SaveCmd])

case class DeleteCmd(id: String) extends AlarmCommand

object DeleteCmd:
  given json: Codec[DeleteCmd] = cmd(Delete.Key, deriveCodec[DeleteCmd])

case class StartCmd(id: String) extends AlarmCommand

object StartCmd:
  given json: Codec[StartCmd] = cmd(Start.Key, deriveCodec[StartCmd])

case class AddWindowsDevice(pushUrl: PushUrl) extends AlarmCommand

object AddWindowsDevice:
  val Key = "push_add"
  given json: Codec[AddWindowsDevice] = cmd(Key, deriveCodec[AddWindowsDevice])

case class RemoveWindowsDevice(url: String) extends AlarmCommand

object RemoveWindowsDevice:
  val Key = "push_remove"
  given json: Codec[RemoveWindowsDevice] = cmd(Key, deriveCodec[RemoveWindowsDevice])

case class RemovePushTag(tag: ServerTag) extends AlarmCommand

object RemovePushTag:
  val Key = "push_remove"
  given json: Codec[RemovePushTag] = cmd(Key, deriveCodec[RemovePushTag])

case class AddGoogleDevice(id: GCMToken, tag: ServerTag) extends AlarmCommand:
  def dest = GCMDevice(id, tag)

object AddGoogleDevice:
  val Key = "gcm_add"
  given json: Codec[AddGoogleDevice] = cmd(Key, deriveCodec[AddGoogleDevice])

case class RemoveGoogleDevice(id: ServerTag) extends AlarmCommand

object RemoveGoogleDevice:
  val Key = "gcm_remove"
  given json: Codec[RemoveGoogleDevice] = cmd(Key, deriveCodec[RemoveGoogleDevice])

case class AddAmazonDevice(id: ADMToken, tag: ServerTag) extends AlarmCommand:
  def dest = ADMDevice(id, tag)

object AddAmazonDevice:
  val Key = "adm_add"
  given json: Codec[AddAmazonDevice] = cmd(Key, deriveCodec[AddAmazonDevice])

case class RemoveAmazonDevice(id: ServerTag) extends AlarmCommand

object RemoveAmazonDevice:
  val Key = "adm_remove"
  given json: Codec[RemoveAmazonDevice] = cmd(Key, deriveCodec[RemoveAmazonDevice])

case class AddApnsDevice(id: APNSToken, tag: ServerTag) extends AlarmCommand:
  def dest = APNSDevice(id, tag)

object AddApnsDevice:
  // I think this JSON format violates the API doc, thus registrations don't work
  val Key = "apns_add"
  given json: Codec[AddApnsDevice] = cmd(Key, deriveCodec[AddApnsDevice])

case class RemoveApnsDevice(id: ServerTag) extends AlarmCommand

object RemoveApnsDevice:
  val Key = "apns_remove"
  given json: Codec[RemoveApnsDevice] = cmd(Key, deriveCodec[RemoveApnsDevice])

case object StopPlayback extends AlarmCommand:
  given json: Codec[StopPlayback.type] = singleCmd(Stop.Key, StopPlayback)

object AlarmCommand:
  given reader: Decoder[AlarmCommand] = json =>
    val v = json.value
    DeleteCmd.json
      .decodeJson(v)
      .orElse(SaveCmd.json.decodeJson(v))
      .orElse(StartCmd.json.decodeJson(v))
      .orElse(StopPlayback.json.decodeJson(v))
      .orElse(AddWindowsDevice.json.decodeJson(v))
      .orElse(RemovePushTag.json.decodeJson(v))
      .orElse(RemoveWindowsDevice.json.decodeJson(v))
      .orElse(AddGoogleDevice.json.decodeJson(v))
      .orElse(RemoveGoogleDevice.json.decodeJson(v))
      .orElse(AddAmazonDevice.json.decodeJson(v))
      .orElse(RemoveAmazonDevice.json.decodeJson(v))
      .orElse(AddApnsDevice.json.decodeJson(v))
      .orElse(RemoveApnsDevice.json.decodeJson(v))
