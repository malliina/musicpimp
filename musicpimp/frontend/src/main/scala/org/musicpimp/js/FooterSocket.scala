package org.musicpimp.js

import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.js.FooterStrings
import com.malliina.musicpimp.js.FrontStrings.HiddenClass
import com.malliina.musicpimp.models.Volume
import org.scalajs.dom.{Element, Event}

import scala.concurrent.duration.Duration

class FooterSocket extends PlaybackSocket with FooterStrings:
  // Why do these need to be lazy?
  lazy val footer = elem(FooterId)
  lazy val playButton = elem(FooterPlay)
  lazy val pauseButton = elem(FooterPause)
  lazy val backwardButton = elem(FooterBackward)
  lazy val forwardButton = elem(FooterForward)
  lazy val playbackButtons = Seq(playButton, pauseButton, backwardButton, forwardButton)

  lazy val progress = elem(FooterProgress)
  lazy val title = elem(FooterTitle)
  lazy val artist = elem(FooterArtist)
  lazy val footerCredit = elem(FooterCredit)

  val NavbarFixedBottom = "navbar-fixed-bottom"

  override def onConnected(e: Event): Unit =
    installHandlers()
    super.onConnected(e)

  def installHandlers() =
    onClick(backwardButton, PrevMsg)
    onClick(playButton, ResumeMsg)
    onClick(pauseButton, StopMsg)
    onClick(forwardButton, NextMsg)

  override def updateTime(duration: Duration): Unit = ()

  override def updatePlayPauseButtons(state: PlayState): Unit =
    updatePlayPause(state)

  override def updateTrack(track: TrackMeta): Unit =
    hide(footerCredit)
    show(backwardButton, forwardButton)
    title.html(track.title)
    artist.html(track.artist)

  override def updatePlaylist(tracks: Seq[TrackMeta]): Unit = ()

  override def updateVolume(vol: Volume): Unit = ()

  override def muteToggled(isMute: Boolean): Unit = ()

  override def onStatus(status: StatusEvent): Unit =
    val track = status.track
    if track.title.nonEmpty then
      updateTrack(track)
      updatePlayPause(status.state)
      ensureClass(footer, FooterFixedClass)
    else
      hidePlaybackFooterShowCredits()
      footer.removeClass(FooterFixedClass)

  def hidePlaybackFooterShowCredits() =
    hide(playbackButtons*)
    show(footerCredit)

  def updatePlayPause(state: PlayState): Unit =
    state match
      case Started =>
        hide(playButton)
        show(pauseButton)
      case _ =>
        hide(pauseButton)
        show(playButton)

  def hide(elems: Element*): Unit =
    elems foreach { elem => ensureClass(elem, HiddenClass) }

  private def ensureClass(elem: Element, clazz: String): Unit =
    if !elem.hasClass(clazz) then elem.addClass(clazz)

  def show(elems: Element*): Unit =
    elems.foreach(_.removeClass(HiddenClass))
