package com.malliina.musicpimp

import java.nio.file.Files

import org.apache.pekko.actor.ActorSystem
import ch.qos.logback.classic.Level
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.log.PimpLog
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.Logging
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SetHasAsScala

class Starter(as: ActorSystem):
  private val log = LoggerFactory.getLogger(getClass)
  val tray = Tray(as)

  def startServices(
    options: InitOptions,
    clouds: Clouds,
    indexer: Indexer,
    schedules: ScheduledPlaybackService,
    lifecycle: ApplicationLifecycle
  )(implicit ec: ExecutionContext): Unit =
    try
      Logging.level = Level.INFO
      FileUtilities.init("musicpimp")
      Files.createDirectories(FileUtil.pimpHomeDir)
      if options.alarms then schedules.init()
      if options.indexer then
        Future:
          indexer.init()
        .recover:
          case e: Exception =>
            log.error(s"Unable to initialize indexer and search", e)
      if options.cloud then clouds.init()
      if options.useTray then tray.installTray(lifecycle)
      val version = BuildInfo.version
      log.info(
        s"Started MusicPimp $version, app dir: ${FileUtil.pimpHomeDir}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}"
      )
    catch
      case e: Exception =>
        log.error(s"Unable to initialize MusicPimp", e)
        throw e

  def stopServices(
    options: InitOptions,
    schedules: ScheduledPlaybackService,
    player: MusicPlayer
  ): Unit =
    log.info("Stopping services...")
    player.close()
    schedules.stop()

  def printThreads(): Unit =
    val threads = Thread.getAllStackTraces.keySet().asScala
    threads.foreach: thread =>
      println("T: " + thread.getName + ", state: " + thread.getState)
    println("Threads in total: " + threads.size)
