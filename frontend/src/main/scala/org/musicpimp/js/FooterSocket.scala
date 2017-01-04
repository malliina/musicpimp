package org.musicpimp.js

import org.musicpimp.js.PlayerState.Started
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQuery

import scala.concurrent.duration.Duration

class FooterSocket extends PlaybackSocket {
  // Why do these need to be defs?
  lazy val playButton: JQuery = elem("footer-play")
  lazy val pauseButton = elem("footer-pause")
  lazy val backwardButton = elem("footer-backward")
  lazy val forwardButton = elem("footer-forward")

  lazy val progress = elem("footer-progress")
  lazy val title = elem("footer-title")
  lazy val artist = elem("footer-artist")
  lazy val footerCredit = elem("footer-credit")

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
    if (!(footerCredit hasClass Hidden))
      footerCredit addClass Hidden
    title html track.title
    artist html track.artist
  }

  override def updatePlaylist(tracks: Seq[Track]) = ()

  override def updateVolume(vol: Int) = ()

  override def muteToggled(isMute: Boolean) = ()

  override def onStatus(status: Status) = {
    updatePlayPause(status.state)
    val track = status.track
    if (track.title.nonEmpty) {
      updateTrack(track)
    }
  }

  def updatePlayPause(state: PlayerState) = {
    state match {
      case Started =>
        //        if (playButton.is(":visible"))
        //          playButton.fadeOut()
        //        if (!pauseButton.is(":visible"))
        //          pauseButton.fadeIn()
        playButton addClass Hidden
        pauseButton removeClass Hidden
      case _ =>

        //        if (pauseButton.is(":visible"))
        //          pauseButton.fadeOut()
        //        if (!playButton.is(":visible"))
        //          playButton.fadeIn()
        pauseButton addClass Hidden
        playButton removeClass Hidden
    }
  }
}
