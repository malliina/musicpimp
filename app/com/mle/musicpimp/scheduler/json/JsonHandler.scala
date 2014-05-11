package com.mle.musicpimp.scheduler.json

import play.api.libs.json.{JsSuccess, JsError, JsResult, JsValue}
import com.mle.util.Log
import com.mle.musicpimp.scheduler.web.SchedulerStrings
import com.mle.musicpimp.scheduler.{ScheduledPlaybackService, ClockPlayback}
import com.mle.musicpimp.audio.MusicPlayer
import com.mle.messaging.mpns.{PushUrls, PushUrl}
import com.mle.messaging.gcm.{GcmUrls, GcmUrl}

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

  case class AddPushUrl(pushUrl: PushUrl) extends Command

  case class RemovePushUrl(id: String) extends Command

  case class AddGcmUrl(gcm: GcmUrl) extends Command

  case class RemoveGcmUrl(id: String) extends Command

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
        json.validate[PushUrl].map(AddPushUrl)
      case PUSH_REMOVE =>
        parse(URL, RemovePushUrl)
      case GCM_ADD =>
        json.validate[GcmUrl].map(AddGcmUrl)
      case GCM_REMOVE =>
        parse(ID, RemoveGcmUrl)
      case cmd =>
        log.info(s"Unknown: $json")
        JsError(s"Unknown command: $cmd")
    }
  }

  private def handleCommand(cmd: Command): Unit = cmd match {
    case Save(ap) => ScheduledPlaybackService.save(ap)
    case Delete(id) => ScheduledPlaybackService.remove(id)
    case Start(id) => ScheduledPlaybackService.find(id).foreach(_.job.run())
    case StopPlayback => musicPlayer.stop()
    case AddPushUrl(url) => PushUrls add url
    case RemovePushUrl(url) => PushUrls removeID url
    case AddGcmUrl(id) => GcmUrls add id
    case RemoveGcmUrl(id) => GcmUrls removeID id
    case _ => log.warn(s"Unknown command: $cmd")
  }
}
