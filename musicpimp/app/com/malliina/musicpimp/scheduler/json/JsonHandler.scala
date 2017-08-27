package com.malliina.musicpimp.scheduler.json

import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler._
import play.api.Logger
import play.api.libs.json._

object AlarmJsonHandler extends JsonHandler

object JsonHandler {
  private val log = Logger(getClass)
}

trait JsonHandler {
  def musicPlayer = MusicPlayer

  def handle(json: JsValue): JsResult[Unit] =
    json.validate[AlarmCommand]
      .map(handleCommand)
      .recover({ case (err: JsError) => log.warn(s"JSON error: '$err'.") })

  def handleCommand(cmd: AlarmCommand): Unit = cmd match {
    case SaveCmd(ap) => ScheduledPlaybackService.save(ap)
    case DeleteCmd(id) => ScheduledPlaybackService.remove(id)
    case StartCmd(id) => ScheduledPlaybackService.find(id).foreach(_.job.run())
    case StopPlayback => musicPlayer.stop()
    case AddWindowsDevice(device) => PushUrls add device
    case RemovePushTag(tag) => PushUrls removeID tag.tag
    case RemoveWindowsDevice(url) => PushUrls removeURL url
    case goog: AddGoogleDevice => GoogleDevices add goog.dest
    case RemoveGoogleDevice(tag) => GoogleDevices removeID tag.tag
    case amzn: AddAmazonDevice => AmazonDevices add amzn.dest
    case RemoveAmazonDevice(tag) => AmazonDevices removeID tag.tag
    case AddApnsDevice(device) => APNSDevices add device
    case RemoveApnsDevice(tag) => APNSDevices removeID tag.tag
    case _ => log.warn(s"Unknown command: $cmd")
  }
}
