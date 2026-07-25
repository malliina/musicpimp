package com.malliina.musicpimp

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher

import java.nio.file.Files
import ch.qos.logback.classic.Level
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.log.PimpLog
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.{AppLogger, Logging}
import play.api.inject.ApplicationLifecycle

import scala.jdk.CollectionConverters.SetHasAsScala

class Starter[F[_]: Async]:
  private val log = AppLogger(getClass)
  val tray = Tray.default()
  val F = Async[F]

  def startServices(
    options: InitOptions,
    clouds: Clouds[F],
    indexer: Indexer[F],
    schedules: ScheduledPlaybackService[F],
    lifecycle: ApplicationLifecycle,
    d: Dispatcher[F]
  ): Unit =
    try
      Logging.level = Level.INFO
      FileUtilities.init("musicpimp")
      Files.createDirectories(FileUtil.pimpHomeDir)
//      if options.alarms then schedules.init()
//      if options.indexer then indexer.initDispatched(d)
//      if options.cloud then clouds.init()
      if options.useTray then tray.installTray()
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
    schedules: ScheduledPlaybackService[F],
    player: MusicPlayer[F]
  ): Unit =
    log.info("Stopping services...")
    player.close()
    schedules.stop()

  def printThreads(): Unit =
    val threads = Thread.getAllStackTraces.keySet().asScala
    threads.foreach: thread =>
      println("T: " + thread.getName + ", state: " + thread.getState)
    println("Threads in total: " + threads.size)
