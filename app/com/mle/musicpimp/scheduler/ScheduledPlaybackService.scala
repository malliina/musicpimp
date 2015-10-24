package com.mle.musicpimp.scheduler

import java.nio.file.{Files, Path}
import java.util.UUID

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.musicpimp.util.FileUtil
import com.mle.util.Log
import play.api.libs.json.Json
import play.api.libs.json.Json._

/**
 *
 * @author mle
 */
trait ScheduledPlaybackService extends Log {
  private val s: IScheduler = Cron4jScheduler
  private val clockAPs = new PlaybackScheduler[ClockSchedule, ClockPlayback](s)

  val persistFile = FileUtil.localPath("schedules.json")

  /**
   * Loads and initializes the saved schedules.
   *
   * You will typically want to call this on program startup.
   */
  def init(): Unit = ()

  start()

  private def start(): Unit = {
    s.start()
    readConf().filter(_.enabled).foreach(clockAPs.schedule)
  }

  def stop(): Unit = {
    s.stop()
    clockAPs.clear()
  }

  def status = readConf()

  def find(id: String) = readConf().find(_.id.contains(id))

  /**
   * Saves or updates action point ´ap´.
   *
   * When updating, we deschedule any previous ap, then reschedule if necessary.
   *
   * @param ap the action point
   * @return
   */
  def save(ap: ClockPlayback): Unit = {
    val withId: ClockPlayback = ap.id
      .filter(id => id != "" && id != "null")
      .fold(ap.copy(id = Some(randomID)))(_ => ap)
    val idOpt = withId.id
    idOpt.foreach(clockAPs.deschedule)
    save(readConf().filter(_.id != idOpt) ++ Seq(withId))
    if (withId.enabled) {
      clockAPs.schedule(withId)
    }
    log debug s"Saved scheduled playback: $ap"
  }

  def remove(id: String) = {
    clockAPs.deschedule(id)
    save(readConf().filter(!_.id.contains(id)))
  }

  private def readConf(): Seq[ClockPlayback] =
    if (Files.isReadable(persistFile)) {
      parseConf(FileUtilities.fileToString(persistFile))
    } else {
      val exists = Files.exists(persistFile)
      val prefix = if (exists) "Cannot read: " else "File does not exist: "
      log.info(s"$prefix${persistFile.toAbsolutePath}, starting from scratch.")
      save(Nil)
      Seq.empty
    }

  def parseConf(json: String): Seq[ClockPlayback] =
    Json.parse(json).validate[Seq[ClockPlayback]].fold(
      invalid => {
        log.warn(s"Ignoring configuration because the JSON is invalid: $invalid")
        Seq.empty
      },
      valid => valid)

  private def save(aps: Seq[ClockPlayback]): Unit = save(aps, persistFile)

  private def save(aps: Seq[ClockPlayback], file: Path): Unit =
    FileUtilities.stringToFile(stringify(toJson(aps)), file)

  private def randomID = UUID.randomUUID().toString
}

object ScheduledPlaybackService extends ScheduledPlaybackService
