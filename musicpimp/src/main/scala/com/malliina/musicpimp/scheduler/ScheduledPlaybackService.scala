package com.malliina.musicpimp.scheduler

import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.implicits.toFunctorOps
import com.malliina.file.FileUtilities
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{MusicPlayer, TrackJson}
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.messaging.TokenService
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService.log
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.AppLogger
import io.circe.syntax.EncoderOps

import java.nio.file.{Files, Path}
import java.util.UUID
import scala.util.Try

object ScheduledPlaybackService:
  private val log = AppLogger(getClass)

  def resource[F[_]: Async](
    player: MusicPlayer[F],
    lib: MusicLibrary[F],
    tokenService: TokenService[F],
    d: Dispatcher[F]
  ): Resource[F, ScheduledPlaybackService[F]] =
    Resource.make(Async[F].delay(ScheduledPlaybackService(player, lib, tokenService, d)))(s =>
      Async[F].delay(s.stop())
    )

class ScheduledPlaybackService[F[_]: Async](
  player: MusicPlayer[F],
  lib: MusicLibrary[F],
  tokenService: TokenService[F],
  val d: Dispatcher[F]
):
  val F = Async[F]

  private val s: IScheduler[F] = Cron4jScheduler[F](d)
  private val clockAPs = new PlaybackScheduler[F, ClockSchedule](s)

  private val persistFile = FileUtil.localPath("schedules2.json")

  /** Loads and initializes the saved schedules.
    *
    * You will typically want to call this on program startup.
    */
  def init(): Unit = ()

  start()

  private def start(): Unit =
    s.start()
    readConf()
      .filter(_.enabled)
      .foreach(conf => clockAPs.schedule(PlaybackJob(conf, player, lib, tokenService)))

  def stop(): Unit =
    s.stop()
    clockAPs.clear()

  def clockList(host: FullUrl): F[Seq[FullClockPlayback]] =
    F.parTraverseN(4)(status)(s => toFull(s, host)).map(_.flatten).map(_.sortBy(_.id))

  private def toFull(conf: ClockPlaybackConf, host: FullUrl): F[Option[FullClockPlayback]] =
    lib
      .track(conf.track)
      .map: maybeTrack =>
        maybeTrack.map: meta =>
          FullClockPlayback(
            conf.id,
            TrackJob(TrackJson.toFull(meta, host)),
            conf.when,
            conf.enabled
          )

  def status: Seq[ClockPlaybackConf] = readConf()

  def find(id: String) = readConf().find(_.id.contains(id))

  def findJob(id: String): Option[PlaybackJob[F]] = find(id).map: conf =>
    PlaybackJob(conf, player, lib, tokenService)

  /** Saves or updates action point ´ap´.
    *
    * When updating, we deschedule any previous ap, then reschedule if necessary.
    *
    * @param ap
    *   the action point
    * @return
    */
  def save(ap: ClockPlaybackConf): Unit =
    val withId: ClockPlaybackConf = ap.id
      .filter(id => id != "" && id != "null")
      .fold(ap.copy(id = Some(randomID)))(_ => ap)
    val idOpt = withId.id
    idOpt.foreach(clockAPs.deschedule)
    save(readConf().filter(_.id != idOpt) ++ Seq(withId))
    if withId.enabled then clockAPs.schedule(PlaybackJob(withId, player, lib, tokenService))
    log.debug(s"Saved scheduled playback: $ap")

  def remove(id: String): Unit =
    clockAPs.deschedule(id)
    save(readConf().filter(!_.id.contains(id)))

  private def readConf(): Seq[ClockPlaybackConf] =
    if Files.isReadable(persistFile) then parseConf(FileUtilities.fileToString(persistFile))
    else
      val exists = Files.exists(persistFile)
      val prefix = if exists then "Cannot read: " else "File does not exist: "
      log.info(s"$prefix${persistFile.toAbsolutePath}, starting from scratch.")
      save(Nil)
      Seq.empty

  private def parseConf(json: String): Seq[ClockPlaybackConf] =
    io.circe.parser
      .decode[Seq[ClockPlaybackConf]](json)
      .left
      .map: err =>
        log.warn(s"Ignoring configuration because the JSON is invalid: $err")
      .getOrElse:
        Seq.empty

  private def save(aps: Seq[ClockPlaybackConf]): Unit = save(aps, persistFile)

  private def save(aps: Seq[ClockPlaybackConf], file: Path): Try[Unit] =
    Try(FileUtilities.stringToFile(aps.asJson.noSpaces, file))

  private def randomID = UUID.randomUUID().toString
