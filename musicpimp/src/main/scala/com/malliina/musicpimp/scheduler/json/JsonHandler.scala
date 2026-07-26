package com.malliina.musicpimp.scheduler.json

import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler.*
import com.malliina.util.AppLogger
import io.circe.Json

object JsonHandler:
  private val log = AppLogger(getClass)

class JsonHandler[F[_]: Sync](
  musicPlayer: MusicPlayer[F],
  val schedules: ScheduledPlaybackService[F]
):
  val F = Sync[F]

  def handle(json: Json): F[Unit] =
    json
      .as[AlarmCommand]
      .fold(
        err =>
          log.warn(s"JSON error: '$err'.")
          F.raiseError(err)
        ,
        ok => handleCommand(ok)
      )

  def handleCommand(cmd: AlarmCommand): F[Unit] = cmd match
    case SaveCmd(ap)              => schedules.save(ap.toConf).pure
    case DeleteCmd(id)            => schedules.remove(id).pure
    case StartCmd(id)             => schedules.findJob(id).map(job => job.run()).getOrElse(F.unit)
    case StopPlayback             => musicPlayer.stop()
    case AddWindowsDevice(device) => PushUrls.add(device).pure.void
    case RemovePushTag(tag)       => PushUrls.removeID(tag.tag).pure
    case RemoveWindowsDevice(url) => PushUrls.removeURL(url).pure
    case goog: AddGoogleDevice    => GoogleDevices.add(goog.dest).pure.void
    case RemoveGoogleDevice(tag)  => GoogleDevices.removeID(tag.tag).pure
    case amzn: AddAmazonDevice    => AmazonDevices.add(amzn.dest).pure.void
    case RemoveAmazonDevice(tag)  => AmazonDevices.removeID(tag.tag).pure
    case apns: AddApnsDevice      => APNSDevices.add(apns.dest).pure.void
    case RemoveApnsDevice(tag)    => APNSDevices.removeID(tag.tag).pure
