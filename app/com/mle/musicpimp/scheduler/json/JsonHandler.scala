package com.mle.musicpimp.scheduler.json

import com.mle.messaging.adm.AmazonDevices
import com.mle.messaging.gcm.GoogleDevices
import com.mle.messaging.mpns.PushUrls
import com.mle.musicpimp.audio.MusicPlayer
import com.mle.musicpimp.scheduler.web.SchedulerStrings
import com.mle.musicpimp.scheduler.{ClockPlayback, ScheduledPlaybackService}
import com.mle.push.gcm.AndroidDevice
import com.mle.push.mpns.PushUrl
import com.mle.util.Log
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}

/**
 *
 * @author mle
 */
trait JsonHandler extends SchedulerStrings with Log {

  def musicPlayer = MusicPlayer

  sealed trait Command

  case class Save(ap: ClockPlayback) extends Command

  case class Delete(id: String) extends Command

  case class Start(id: String) extends Command

  case class AddWindowsDevice(pushUrl: PushUrl) extends Command

  case class RemoveWindowsDevice(url: String) extends Command

  case class RemovePushTag(tag: String) extends Command

  case class AddGoogleDevice(dest: AndroidDevice) extends Command

  case class RemoveGoogleDevice(id: String) extends Command

  case class AddAmazonDevice(dest: AndroidDevice) extends Command

  case class RemoveAmazonDevice(id: String) extends Command

  case object StopPlayback extends Command

  def handle(json: JsValue): JsResult[Unit] = parseCommand(json) map handleCommand

  private def parseCommand(json: JsValue): JsResult[Command] = {
    def parse[T](key: String, f: String => T): JsResult[T] =
      (json \ key).validate[String].map(f)

    (json \ CMD).validate[String].flatMap {
      case DELETE =>
        parse(ID, Delete)
      case SAVE =>
        (json \ AP).validate[ClockPlayback].map(Save)
      case START =>
        parse(ID, Start)
      case STOP =>
        JsSuccess(StopPlayback)
      case PUSH_ADD =>
        json.validate[PushUrl].map(AddWindowsDevice)
      case PUSH_REMOVE =>
        (json \ TAG).validate[String].map(RemovePushTag) orElse (json \ URL).validate[String].map(RemoveWindowsDevice)
      case GCM_ADD =>
        json.validate[AndroidDevice].map(AddGoogleDevice)
      case GCM_REMOVE =>
        parse(ID, RemoveGoogleDevice)
      case ADM_ADD =>
        json.validate[AndroidDevice].map(AddAmazonDevice)
      case ADM_REMOVE =>
        parse(ID, RemoveAmazonDevice)
      case cmd =>
        log.warn(s"Unknown JSON: $json")
        JsError(s"Unknown command: $cmd")
    }
  }

  private def handleCommand(cmd: Command): Unit = cmd match {
    case Save(ap) => ScheduledPlaybackService.save(ap)
    case Delete(id) => ScheduledPlaybackService.remove(id)
    case Start(id) => ScheduledPlaybackService.find(id).foreach(_.job.run())
    case StopPlayback => musicPlayer.stop()
    case AddWindowsDevice(device) => PushUrls add device
    case RemovePushTag(tag) => PushUrls removeID tag
    case RemoveWindowsDevice(url) => PushUrls removeURL url
    case AddGoogleDevice(device) => GoogleDevices add device
    case RemoveGoogleDevice(id) => GoogleDevices removeID id
    case AddAmazonDevice(device) => AmazonDevices add device
    case RemoveAmazonDevice(id) => AmazonDevices removeID id
    case _ => log.warn(s"Unknown command: $cmd")
  }
}
