package com.malliina.musicpimp

import java.awt.Desktop
import java.net.URI
import java.nio.file.{Files, Paths}
import java.rmi.ConnectException

import ch.qos.logback.classic.Level
import com.malliina.concurrent.ExecutionContexts
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
import com.malliina.rmi.{RmiClient, RmiServer, RmiUtil}
import com.malliina.util.{Log, Logging, Scheduling}
import play.core.server.{ProdServerStart, RealServerProcess, ReloadableServer}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

// TODO get rid of this object entirely, especially Netty
object Starter extends PlayLifeCycle with Log {

  override def appName: String = "musicpimp"

  var server: Option[ReloadableServer] = None

  var rmiServer: Option[RmiServer] = None

  def main(args: Array[String]) {
    args.headOption match {
      case Some("stop") =>
        try {
          RmiClient.launchClient()
          // this hack allows the System.exit() call in the stop method eventually to run before we exit
          // obviously we can't await it from another vm
          Thread sleep 2000
        } catch {
          case _: ConnectException =>
            log warn "Unable to stop; perhaps MusicPimp is already stopped?"
        }
      case anythingElse => start()
    }
  }

  def start() = {
    // Init conf
    sys.props += ("pidfile.path" -> "/dev/null")
    FileUtilities.basePath = Paths get sys.props.get(s"$appName.home").getOrElse(sys.props("user.dir"))
    log info s"Starting $appName... app home: ${FileUtilities.basePath}"
    sys.props ++= conformize(readConfFile(appName))
    validateKeyStoreIfSpecified()
    // Start server
    val process = new RealServerProcess(Nil)
    val s = ProdServerStart.start(process)
    server = Some(s)
    // Init RMI
    RmiUtil.initSecurityPolicy()
    rmiServer = Some(new RmiServer() {
      override def onClosed() {
        stop()
      }
    })
    Tray.installTray()
  }

  def stop() {
    log info "Stopping MusicPimp..."
    try {
      // will call Global.onStop, which calls stopServices()
      server.foreach(_.stop())
    } finally {
      /** Well the following is lame, but some threads are left hanging
        * and I don't know how to stop them gracefully.
        */
      Future(System.exit(0))(ExecutionContexts.cached)
    }
  }

  def startServices(options: InitOptions, clouds: Clouds, db: PimpDb, indexer: Indexer)(implicit ec: ExecutionContext): Unit = {
    try {
      Logging.level = Level.INFO
      FileUtilities init "musicpimp"
      Files.createDirectories(FileUtil.pimpHomeDir)
      if (options.alarms) {
        ScheduledPlaybackService.init()
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

  def stopServices(options: InitOptions) = {
    log.info("Stopping services...")
    MusicPlayer.close()
    Scheduling.shutdown()
//    if(options.alarms) {
      ScheduledPlaybackService.stop()
//    }
    //    Search.subscription.unsubscribe()
  }

  def printThreads() {
    val threads = Thread.getAllStackTraces.keySet().asScala
    threads.foreach { thread =>
      println("T: " + thread.getName + ", state: " + thread.getState)
    }
    println("Threads in total: " + threads.seq.size)
  }

  def openWebInterface() = {
    val address = sys.props.get(httpAddressKey) getOrElse "localhost"
    val (protocol, port) =
      tryReadInt(httpsPortKey).map(p => ("https", p)) orElse
        tryReadInt(httpPortKey).map(p => ("http", p)) getOrElse
        (("http", 9000))
    Desktop.getDesktop.browse(new URI(s"$protocol://$address:$port"))
  }
}
