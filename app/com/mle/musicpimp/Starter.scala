package com.mle.musicpimp

import java.awt.Desktop
import java.net.URI
import java.rmi.ConnectException
import java.util.concurrent.TimeUnit

import com.mle.musicpimp.audio.MusicPlayer
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.play.PlayLifeCycle
import com.mle.rmi.{RmiClient, RmiServer, RmiUtil}
import com.mle.util.{Log, Scheduling}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author mle
 */
object Starter extends PlayLifeCycle with Log {

  override def appName: String = "musicpimp"

  var rmiServer: Option[RmiServer] = None

  override def main(args: Array[String]) {
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

  override def start() = {
    super.start()
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
