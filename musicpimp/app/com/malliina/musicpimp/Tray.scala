package com.malliina.musicpimp

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import java.net.URI

import akka.actor.CoordinatedShutdown.JvmExitReason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import com.malliina.util.{Util, Utils}
import javax.swing.{ImageIcon, UIManager}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Try

object Tray {
  def apply(as: ActorSystem) = new Tray(as)
}

/**
  * @see http://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html
  */
class Tray(as: ActorSystem) {
  private val log = Logger(getClass)
  val iconResource = "guitar-16x16.png"
  protected val (httpPortKey, httpsPortKey, httpAddressKey) =
    ("http.port", "https.port", "http.address")
  protected val defaultHttpPort = 9000
  protected val defaultHttpAddress = "0.0.0.0"

  /** Installs a system tray item with the MusicPimp logo which opens a popup menu allowing the user to Open/Stop
    * MusicPimp.
    */
  def installTray(lifecycle: ApplicationLifecycle): Unit = {
    if (SystemTray.isSupported) {
      Try(UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName))
      val popup = new PopupMenu()
      popup add menuItem("Open", openWebInterface())
      popup add menuItem("Stop", stop(as))
      val trayIcon = new TrayIcon(icon(iconResource, "MusicPimp"), "Open MusicPimp", popup)
      trayIcon setImageAutoSize true
      // commented because is triggered on all clicks on OSX, overriding the other listeners
      //      trayIcon addActionListener actionListener(Starter.openWebInterface())

      // commented because does not combine well with the popup menu
      //      trayIcon.addMouseListener(new MouseAdapter {
      //        override def mouseClicked(e: MouseEvent): Unit = {
      //          if (e.getClickCount == 1 && e.getButton == MouseEvent.BUTTON1) {
      //            Starter.openWebInterface()
      //          }
      //        }
      //      })
      val tray = SystemTray.getSystemTray
      Try(tray add trayIcon)
        .map(_ => {
          //        trayIcon.displayMessage("MusicPimp", "MusicPimp is now running.", TrayIcon.MessageType.INFO)
        })
        .toOption
        .fold(log.warn(s"Unable to add tray icon."))(_ => log.info(s"Added tray icon."))
    } else {
      log warn s"System tray is not supported."
    }
  }

  def openWebInterface(): Unit = {
    val address = sys.props.get(httpAddressKey) getOrElse "localhost"
    val (protocol, port) =
      tryReadInt(httpsPortKey).map(p => ("https", p)) orElse
        tryReadInt(httpPortKey).map(p => ("http", p)) getOrElse
        (("http", 9000))
    Desktop.getDesktop.browse(new URI(s"$protocol://$address:$port"))
  }

  def stop(as: ActorSystem): Unit = {
    Await.result(CoordinatedShutdown(as).run(JvmExitReason), 5.seconds)
    System.exit(0)
  }

  private def menuItem(label: String, onClick: => Unit) = {
    val item = new MenuItem(label)
    item addActionListener actionListener(onClick)
    item
  }

  private def icon(path: String, desc: String) = {
    val url = Util.resource(path)
    new ImageIcon(url, desc).getImage
  }

  private def actionListener(code: => Unit) = new ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = {
      code
    }
  }

  protected def tryReadInt(key: String): Option[Int] =
    sys.props.get(key).filter(_ != "disabled").flatMap { ps =>
      Utils.opt[Int, NumberFormatException](Integer.parseInt(ps))
    }
}
