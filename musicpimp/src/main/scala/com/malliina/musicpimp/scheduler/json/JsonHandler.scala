package com.malliina.musicpimp.scheduler.json

import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler.*
import io.circe.{Decoder, Json}
import play.api.Logger

object JsonHandler:
  private val log = Logger(getClass)

class JsonHandler(musicPlayer: MusicPlayer, val schedules: ScheduledPlaybackService):
  def handle(json: Json): Decoder.Result[Unit] =
    json
      .as[AlarmCommand]
      .map(handleCommand)
      .left
      .map: err =>
        log.warn(s"JSON error: '$err'.")
        err

  def handleCommand(cmd: AlarmCommand): Unit = cmd match
    case SaveCmd(ap)              => schedules.save(ap.toConf)
    case DeleteCmd(id)            => schedules.remove(id)
    case StartCmd(id)             => schedules.findJob(id).foreach(_.run())
    case StopPlayback             => musicPlayer.stop()
    case AddWindowsDevice(device) => PushUrls.add(device)
    case RemovePushTag(tag)       => PushUrls.removeID(tag.tag)
    case RemoveWindowsDevice(url) => PushUrls.removeURL(url)
    case goog: AddGoogleDevice    => GoogleDevices.add(goog.dest)
    case RemoveGoogleDevice(tag)  => GoogleDevices.removeID(tag.tag)
    case amzn: AddAmazonDevice    => AmazonDevices.add(amzn.dest)
    case RemoveAmazonDevice(tag)  => AmazonDevices.removeID(tag.tag)
    case apns: AddApnsDevice      => APNSDevices.add(apns.dest)
    case RemoveApnsDevice(tag)    => APNSDevices.removeID(tag.tag)
