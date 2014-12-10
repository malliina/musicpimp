package com.mle.musicpimp

import java.awt._
import java.awt.event.{ActionEvent, ActionListener, MouseAdapter, MouseEvent}
import javax.swing.ImageIcon

import com.mle.util.{Log, Util}

import scala.util.Try

/**
 * @author Michael
 * @see http://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html
 */
object Tray extends Log {
  val iconResource = "guitar-16x16.png"

  /**
   * Installs a system tray that displays the MusicPimp logo and opens the web interface when left-clicked, and displays
   * a popup menu allowing the user to Open/Stop MusicPimp when right-clicked.
   */
  def installTray() = {
    if (SystemTray.isSupported) {
      val popup = new PopupMenu()
      popup add menuItem("Open", Starter.openWebInterface())
      popup add menuItem("Stop", Starter.stop())
      val trayIcon = new TrayIcon(icon(iconResource, "MusicPimp"), "Open MusicPimp", popup)
      trayIcon setImageAutoSize true
      trayIcon addActionListener actionListener(Starter.openWebInterface())
      trayIcon.addMouseListener(new MouseAdapter {
        override def mouseClicked(e: MouseEvent): Unit = {
          if (e.getClickCount == 1 && e.getButton == MouseEvent.BUTTON1) {
            Starter.openWebInterface()
          }
        }
      })
      val tray = SystemTray.getSystemTray
      Try(tray add trayIcon).map(_ => {
        trayIcon.displayMessage("MusicPimp", "MusicPimp is now running.", TrayIcon.MessageType.INFO)
      }).toOption.fold(log.warn(s"Unable to add tray icon."))(_ => log.info(s"Added tray icon."))
    } else {
      log warn s"System tray is not supported."
    }
  }

  def menuItem(label: String, onClick: => Unit) = {
    val item = new MenuItem(label)
    item addActionListener actionListener(onClick)
    item
  }

  def icon(path: String, desc: String) = {
    val url = Util.resource(path)
    new ImageIcon(url, desc).getImage
  }

  def actionListener(code: => Unit) = new ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = code
  }
}
