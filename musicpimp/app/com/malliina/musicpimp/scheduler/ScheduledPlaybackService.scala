package com.malliina.musicpimp.scheduler

import java.nio.file.{Files, Path}
import java.util.UUID

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.FileUtilities
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.util.FileUtil
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json._

import scala.concurrent.Future

class ScheduledPlaybackService(lib: MusicLibrary) {
  private val log = Logger(getClass)

  private val s: IScheduler = Cron4jScheduler
  private val clockAPs = new PlaybackScheduler[ClockSchedule](s)

  val persistFile = FileUtil.localPath("schedules2.json")

  /** Loads and initializes the saved schedules.
    *
    * You will typically want to call this on program startup.
    */
  def init(): Unit = ()

  start()

  private def start(): Unit = {
    s.start()
    readConf().filter(_.enabled).foreach(conf => clockAPs.schedule(PlaybackJob(conf, lib)))
  }

  def stop(): Unit = {
    s.stop()
    clockAPs.clear()
  }

  def clockList(host: FullUrl) = Future.traverse(status)(toFull(_, host)).map(_.flatten)

  private def toFull(conf: ClockPlaybackConf, host: FullUrl): Future[Option[FullClockPlayback]] =
    lib.track(conf.track).map { maybeTrack =>
      maybeTrack.map { meta =>
        FullClockPlayback(conf.id, TrackJob(TrackJson.toFull(meta, host)), conf.when, conf.enabled)
      }
    }

  def status: Seq[ClockPlaybackConf] = readConf()

  def find(id: String) = readConf().find(_.id.contains(id))

  def findJob(id: String) = find(id).map { conf => PlaybackJob(conf, lib) }

  /** Saves or updates action point ´ap´.
    *
    * When updating, we deschedule any previous ap, then reschedule if necessary.
    *
    * @param ap the action point
    * @return
    */
  def save(ap: ClockPlaybackConf): Unit = {
    val withId: ClockPlaybackConf = ap.id
      .filter(id => id != "" && id != "null")
      .fold(ap.copy(id = Some(randomID)))(_ => ap)
    val idOpt = withId.id
    idOpt.foreach(clockAPs.deschedule)
    save(readConf().filter(_.id != idOpt) ++ Seq(withId))
    if (withId.enabled) {
      clockAPs.schedule(PlaybackJob(withId, lib))
    }
    log debug s"Saved scheduled playback: $ap"
  }

  def remove(id: String): Unit = {
    clockAPs.deschedule(id)
    save(readConf().filter(!_.id.contains(id)))
  }

  private def readConf(): Seq[ClockPlaybackConf] =
    if (Files.isReadable(persistFile)) {
      parseConf(FileUtilities.fileToString(persistFile))
    } else {
      val exists = Files.exists(persistFile)
      val prefix = if (exists) "Cannot read: " else "File does not exist: "
      log.info(s"$prefix${persistFile.toAbsolutePath}, starting from scratch.")
      save(Nil)
      Seq.empty
    }

  def parseConf(json: String): Seq[ClockPlaybackConf] = {
    Json.parse(json).validate[Seq[ClockPlaybackConf]].fold(
      invalid => {
        log.warn(s"Ignoring configuration because the JSON is invalid: $invalid")
        Seq.empty
      },
      valid => valid)
  }

  private def save(aps: Seq[ClockPlaybackConf]): Unit = save(aps, persistFile)

  private def save(aps: Seq[ClockPlaybackConf], file: Path): Unit = {
    FileUtilities.stringToFile(stringify(toJson(aps)), file)
  }

  private def randomID = UUID.randomUUID().toString
}
