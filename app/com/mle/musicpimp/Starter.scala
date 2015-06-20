package com.mle.musicpimp

import java.awt.Desktop
import java.net.URI
import java.nio.file.{Files, Paths}
import java.rmi.ConnectException

import ch.qos.logback.classic.Level
import com.mle.file.FileUtilities
import com.mle.musicpimp.audio.MusicPlayer
import com.mle.musicpimp.auth.Auth
import com.mle.musicpimp.cloud.Clouds
import com.mle.musicpimp.db.{DatabaseUserManager, Indexer, PimpDb}
import com.mle.musicpimp.log.PimpLog
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.musicpimp.util.FileUtil
import com.mle.play.PlayLifeCycle
import com.mle.rmi.{RmiClient, RmiServer, RmiUtil}
import com.mle.util.{Log, Logging, Scheduling}
import controllers.Search
import play.core.server.{ProdServerStart, RealServerProcess, ServerWithStop}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author mle
 */
object Starter extends PlayLifeCycle with Log {

  override def appName: String = "musicpimp"

  var server: Option[ServerWithStop] = None

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
          case ce: ConnectException =>
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
      /**
       * Well the following is lame, but some threads are left hanging
       * and I don't know how to stop them gracefully.
       *
       * Likely guilty: play! framework, because if no web requests have
       * been made, the app exits normally without this
       */
      Future(System.exit(0))
    }
  }

  def startServices() = {
    try {
      Logging.level = Level.INFO
      FileUtilities init "musicpimp"
      Files.createDirectories(FileUtil.pimpHomeDir)
      ScheduledPlaybackService.init()
      PimpDb.init()
      Auth.migrateFileCredentialsToDatabaseIfExists()
      new DatabaseUserManager().ensureAtLeastOneUserExists()
      Future {
        Indexer.init()
        Search.init()
      }.recover {
        case e: Exception =>
          log.error(s"Unable to initialize indexer and search", e)
      }
      Clouds.init()
      val version = com.mle.musicpimp.BuildInfo.version
      log info s"Started MusicPimp $version, app dir: ${FileUtil.pimpHomeDir}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}"
    } catch {
      case e: Exception =>
        log.error(s"Unable to initialize MusicPimp", e)
        throw e
    }
  }
  def stopServices() = {
    log.info("Stopping services...")
    MusicPlayer.close()
    Scheduling.shutdown()
    ScheduledPlaybackService.stop()
    Search.subscription.unsubscribe()
    nettyServer foreach (_.stop())
  }

  def printThreads() {
    val threads = Thread.getAllStackTraces.keySet()
    threads.foreach(thread => {
      println("T: " + thread.getName + ", state: " + thread.getState)
    })
    println("Threads in total: " + threads.size())
  }

  def openWebInterface() = {
    val address = sys.props.get(httpAddressKey) getOrElse "localhost"
    val (protocol, port) =
      tryReadInt(httpsPortKey).map(p => ("https", p)) orElse
        tryReadInt(httpPortKey).map(p => ("http", p)) getOrElse
        (("http", 9000))
    Desktop.getDesktop.browse(new URI(s"$protocol://$address:$port"))
  }

  //  def test() {
  //    future {
  //      var loop = true
  //      while (loop) {
  //        printThreads()
  //        val line = Console.readLine("Press enter to print threads, q followed by enter to quit")
  //        loop = "q" != line
  //      }
  //    }
  //  }
}
