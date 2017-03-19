package org.musicpimp.js

import org.musicpimp.js.PlayerState.Started
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQuery
import com.malliina.musicpimp.js.FooterStrings
import scala.concurrent.duration.Duration

class FooterSocket extends PlaybackSocket with FooterStrings {
  // Why do these need to be lazy?
  lazy val playButton: JQuery = elem(FooterPlay)
  lazy val pauseButton = elem(FooterPause)
  lazy val backwardButton = elem(FooterBackward)
  lazy val forwardButton = elem(FooterForward)
  lazy val playbackButtons = Seq(playButton, pauseButton, backwardButton, forwardButton)

  lazy val progress = elem(FooterProgress)
  lazy val title = elem(FooterTitle)
  lazy val artist = elem(FooterArtist)
  lazy val footerCredit = elem(FooterCredit)
  lazy val bottomNavbar = elem(BottomNavbar)

  val NavbarFixedBottom = "navbar-fixed-bottom"

  override def onConnected(e: Event) = {
    installHandlers()
    super.onConnected(e)
  }

  def installHandlers() = {
    onClick(backwardButton, Playback.prev)
    onClick(playButton, Playback.resume)
    onClick(pauseButton, Playback.stop)
    onClick(forwardButton, Playback.next)
  }

  override def updateTime(duration: Duration) = ()

  override def updatePlayPauseButtons(state: PlayerState) = {
    updatePlayPause(state)
  }

  override def updateTrack(track: Track) = {
    hide(footerCredit)
    show(backwardButton, forwardButton)
    ensureClass(bottomNavbar, NavbarFixedBottom)
    title html track.title
    artist html track.artist
  }

  override def updatePlaylist(tracks: Seq[Track]) = ()

  override def updateVolume(vol: Int) = ()

  override def muteToggled(isMute: Boolean) = ()

  override def onStatus(status: Status) = {
    val track = status.track
    if (track.title.nonEmpty) {
      updateTrack(track)
      updatePlayPause(status.state)
    } else {
      hidePlaybackFooterShowCredits()
    }
  }

  def hidePlaybackFooterShowCredits() = {
    hide(playbackButtons: _*)
    show(footerCredit)
    bottomNavbar removeClass NavbarFixedBottom
  }

  def updatePlayPause(state: PlayerState) = {
    state match {
      case Started =>
        hide(playButton)
        show(pauseButton)
      case _ =>
        hide(pauseButton)
        show(playButton)
    }
  }

  def hide(elems: JQuery*) =
    elems foreach { elem => ensureClass(elem, Hidden) }

  def ensureClass(elem: JQuery, clazz: String) =
    if (!(elem hasClass clazz)) elem addClass clazz

  def show(elems: JQuery*) =
    elems foreach (_ removeClass Hidden)
}
