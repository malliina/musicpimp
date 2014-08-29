package com.mle.musicpimp

import java.nio.file.Paths
import java.rmi.ConnectException
import java.util.concurrent.TimeUnit

import com.mle.musicpimp.audio.MusicPlayer
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.musicpimp.util.FileUtil
import com.mle.rmi.{RmiClient, RmiServer, RmiUtil}
import com.mle.util.{FileUtilities, Log, Scheduling, Util}
import play.core.StaticApplication
import play.core.server.NettyServer

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 *
 * @author mle
 */
object Starter extends Log {
  var rmiServer: Option[RmiServer] = None
  var nettyServer: Option[NettyServer] = None

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

  def start() {
    //    LogbackUtils.installAppender(RxLogbackAppender)
    log info "Starting MusicPimp ..."
    RmiUtil.initSecurityPolicy()
    rmiServer = Some(new RmiServer() {
      override def onClosed() {
        stop()
      }
    })
    FileUtilities.basePath = Paths get sys.props.get("musicpimp.home").getOrElse(sys.props("user.dir"))
    // adds settings in musicpimp.conf to system properties
    val confFile = FileUtilities.pathTo("musicpimp.conf")
    val props = FileUtil.props(confFile)
    // makes keystore file path absolute
    val keystorePathKey = "https.keyStore"
    val sysPropsAdditions = props.get(keystorePathKey).map(keyStorePath => {
      props.updated(keystorePathKey, FileUtilities.pathTo(keyStorePath).toAbsolutePath.toString)
    }).getOrElse(props)
    sys.props ++= sysPropsAdditions

    /**
     * NettyServer.createServer insists on writing a RUNNING_PID file.
     * Fuck that.
     */
    nettyServer = Some(createServer())
  }

  def createServer() = {
    val server = new NettyServer(
      new StaticApplication(FileUtilities.basePath.toFile),
      Option(System.getProperty("http.port")).map(Integer.parseInt).orElse(Some(9000)),
      Option(System.getProperty("https.port")).map(Integer.parseInt),
      Option(System.getProperty("http.address")).getOrElse("0.0.0.0")
    )
    Util.addShutdownHook(server.stop())
    server
  }

  def stop() {
    log info "Stopping MusicPimp..."
    try {
      MusicPlayer.close()
      Scheduling.shutdown()
      ScheduledPlaybackService.stop()
      nettyServer foreach (server => {
        server.allChannels.close().await(2, TimeUnit.SECONDS)
        server.stop()
      })
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

  def printThreads() {
    val threads = Thread.getAllStackTraces.keySet()
    threads.foreach(thread => {
      println("T: " + thread.getName + ", state: " + thread.getState)
    })
    println("Threads in total: " + threads.size())
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
