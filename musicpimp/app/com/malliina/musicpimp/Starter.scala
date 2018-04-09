package com.malliina.musicpimp

import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.rmi.ConnectException

import ch.qos.logback.classic.Level
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.app.InitOptions
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.auth.Auth
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.db.{DatabaseUserManager, Indexer, PimpDb}
import com.malliina.musicpimp.log.PimpLog
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.PlayLifeCycle
import com.malliina.rmi.RmiClient
import com.malliina.util.{Logging, Utils}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object Starter extends PlayLifeCycle("musicpimp", 2666) {
  private val log = LoggerFactory.getLogger(getClass)

  override def start(): Unit = {
    super.start()
    Tray.installTray()
  }

  override def main(args: Array[String]) {
    args.headOption match {
      case Some("stop") =>
        try {
          RmiClient.launchClient()
          // this hack allows the System.exit() call in the stop method eventually to run before we exit
          // obviously we can't await it from another vm
          Thread sleep 12000
        } catch {
          case _: ConnectException =>
            log warn "Unable to stop; perhaps MusicPimp is already stopped?"
        }
      case anythingElse => start()
    }
  }

  def startServices(options: InitOptions, clouds: Clouds, db: PimpDb, indexer: Indexer, schedules: ScheduledPlaybackService)(implicit ec: ExecutionContext): Unit = {
    try {
      Logging.level = Level.INFO
      FileUtilities init "musicpimp"
      Files.createDirectories(FileUtil.pimpHomeDir)
      if (options.alarms) {
        schedules.init()
      }
      if (options.database) {
        db.init()
        new Auth(db).migrateFileCredentialsToDatabaseIfExists()
        new DatabaseUserManager(db).ensureAtLeastOneUserExists()
      }
      if (options.indexer) {
        Future {
          indexer.init()
        }.recover {
          case e: Exception =>
            log.error(s"Unable to initialize indexer and search", e)
        }
      }
      if (options.cloud) {
        clouds.init()
      }
      val version = com.malliina.musicpimp.BuildInfo.version
      log info s"Started MusicPimp $version, app dir: ${FileUtil.pimpHomeDir}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}"
    } catch {
      case e: Exception =>
        log.error(s"Unable to initialize MusicPimp", e)
        throw e
    }
  }

  def stopServices(options: InitOptions, schedules: ScheduledPlaybackService): Unit = {
    log.info("Stopping services...")
    MusicPlayer.close()
    schedules.stop()
  }

  def printThreads() {
    val threads = Thread.getAllStackTraces.keySet().asScala
    threads.foreach { thread =>
      println("T: " + thread.getName + ", state: " + thread.getState)
    }
    println("Threads in total: " + threads.seq.size)
  }

  def openWebInterface(): Unit = {
    val address = sys.props.get(httpAddressKey) getOrElse "localhost"
    val (protocol, port) =
      tryReadInt(httpsPortKey).map(p => ("https", p)) orElse
        tryReadInt(httpPortKey).map(p => ("http", p)) getOrElse
        (("http", 9000))
    Desktop.getDesktop.browse(new URI(s"$protocol://$address:$port"))
  }

  protected def tryReadInt(key: String): Option[Int] =
    sys.props.get(key).filter(_ != "disabled").flatMap(ps => Utils.opt[Int, NumberFormatException](Integer.parseInt(ps)))
}
