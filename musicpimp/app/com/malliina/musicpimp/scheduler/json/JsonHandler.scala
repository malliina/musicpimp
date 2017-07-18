package com.malliina.musicpimp.scheduler.json

import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.adm.{ADMDevice, AmazonDevices}
import com.malliina.musicpimp.messaging.apns.{APNSDevice, APNSDevices}
import com.malliina.musicpimp.messaging.gcm.{GCMDevice, GoogleDevices}
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.json.JsonHandler._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.{ClockPlayback, ScheduledPlaybackService}
import com.malliina.push.mpns.PushUrl
import play.api.Logger
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}

object AlarmJsonHandler extends JsonHandler

object JsonHandler {
  private val log = Logger(getClass)
}

trait JsonHandler extends SchedulerStrings {

  def musicPlayer = MusicPlayer

  sealed trait Command

  case class SaveCmd(ap: ClockPlayback) extends Command

  case class DeleteCmd(id: String) extends Command

  case class StartCmd(id: String) extends Command

  case class AddWindowsDevice(pushUrl: PushUrl) extends Command

  case class RemoveWindowsDevice(url: String) extends Command

  case class RemovePushTag(tag: ServerTag) extends Command

  case class AddGoogleDevice(dest: GCMDevice) extends Command

  case class RemoveGoogleDevice(id: ServerTag) extends Command

  case class AddAmazonDevice(dest: ADMDevice) extends Command

  case class RemoveAmazonDevice(id: ServerTag) extends Command

  case class AddApnsDevice(id: APNSDevice) extends Command

  case class RemoveApnsDevice(id: ServerTag) extends Command

  case object StopPlayback extends Command

  def handle(json: JsValue): JsResult[Unit] = parseCommand(json) map handleCommand

  private def parseCommand(json: JsValue): JsResult[Command] = {
    def parse[T](key: String, f: String => T): JsResult[T] =
      (json \ key).validate[String].map(f)

    def parseTag[T](f: ServerTag => T): JsResult[T] = (json \ Id).validate[ServerTag].map(f)

    val result = (json \ Cmd).validate[String].flatMap {
      case Delete =>
        parse(Id, DeleteCmd)
      case Save =>
        implicit val reader = ClockPlayback.shortJson
        (json \ Ap).validate[ClockPlayback].map(SaveCmd)
      case Start =>
        parse(Id, StartCmd)
      case Stop =>
        JsSuccess(StopPlayback)
      case PUSH_ADD =>
        json.validate[PushUrl].map(AddWindowsDevice)
      case PUSH_REMOVE =>
        (json \ TAG).validate[ServerTag].map(RemovePushTag) orElse (json \ URL).validate[String].map(RemoveWindowsDevice)
      case GCM_ADD =>
        json.validate[GCMDevice].map(AddGoogleDevice)
      case GCM_REMOVE =>
        parseTag(RemoveGoogleDevice)
      case ADM_ADD =>
        json.validate[ADMDevice].map(AddAmazonDevice)
      case ADM_REMOVE =>
        parseTag(RemoveAmazonDevice)
      case ApnsAdd =>
        json.validate[APNSDevice].map(AddApnsDevice)
      case ApnsRemove =>
        parseTag(RemoveApnsDevice)
      case cmd =>
        JsError(s"Unknown command: $cmd")
    }
    if (result.isError) {
      log.warn(s"Invalid JSON: $json")
    }
    result
  }

  private def handleCommand(cmd: Command): Unit = cmd match {
    case SaveCmd(ap) => ScheduledPlaybackService.save(ap)
    case DeleteCmd(id) => ScheduledPlaybackService.remove(id)
    case StartCmd(id) => ScheduledPlaybackService.find(id).foreach(_.job.run())
    case StopPlayback => musicPlayer.stop()
    case AddWindowsDevice(device) => PushUrls add device
    case RemovePushTag(tag) => PushUrls removeID tag.tag
    case RemoveWindowsDevice(url) => PushUrls removeURL url
    case AddGoogleDevice(device) => GoogleDevices add device
    case RemoveGoogleDevice(tag) => GoogleDevices removeID tag.tag
    case AddAmazonDevice(device) => AmazonDevices add device
    case RemoveAmazonDevice(tag) => AmazonDevices removeID tag.tag
    case AddApnsDevice(device) => APNSDevices add device
    case RemoveApnsDevice(tag) => APNSDevices removeID tag.tag
    case _ => log.warn(s"Unknown command: $cmd")
  }
}
