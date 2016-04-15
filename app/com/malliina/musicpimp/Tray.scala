package com.malliina.musicpimp

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.{ImageIcon, UIManager}

import com.malliina.util.{Log, Util}

import scala.util.Try

/**
  * @see http://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html
  */
object Tray extends Log {
  val iconResource = "guitar-16x16.png"

  /** Installs a system tray item with the MusicPimp logo which opens a popup menu allowing the user to Open/Stop
    * MusicPimp.
    */
  def installTray() = {
    if (SystemTray.isSupported) {
      Try(UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName))
      val popup = new PopupMenu()
      popup add menuItem("Open", Starter.openWebInterface())
      popup add menuItem("Stop", Starter.stop())
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
      Try(tray add trayIcon).map(_ => {
        //        trayIcon.displayMessage("MusicPimp", "MusicPimp is now running.", TrayIcon.MessageType.INFO)
      }).toOption.fold(log.warn(s"Unable to add tray icon."))(_ => log.info(s"Added tray icon."))
    } else {
      log warn s"System tray is not supported."
    }
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
}
