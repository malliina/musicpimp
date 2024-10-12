package org.musicpimp.js

import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.js.PlayerStrings
import com.malliina.musicpimp.json.PlaybackStrings
import com.malliina.musicpimp.models.Volume
import scalatags.JsDom.all.*

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js

object Playback extends PlaybackStrings:
  val SocketUrl = "/ws/playback?f=json"

  def volume(vol: Int) = ValuedCommand(VolumeKey, vol)
  def seek(pos: Int) = ValuedCommand(Seek, pos)
  def skip(idx: Int) = ValuedCommand(Skip, idx)

  /** @param duration
    *   a duration of time
    * @return
    *   "HH:mm:ss" if `duration` >= 1 hour, otherwise "mm:ss"
    */
  def toHHMMSS(duration: Duration) =
    val s = duration.toSeconds
    val hours = duration.toHours
    if hours > 0 then "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    else "%02d:%02d".format((s % 3600) / 60, s % 60)

class Playback extends PlaybackSocket with PlayerStrings:
  val OptionKey = "option"
  val Max = "max"
  val Min = "min"
  val SongClass = "song"
  val playerDiv = elem(PlayerDivId)
  val durationElem = elem(DurationId)
  val sliderElem = MyJQuery(s"#$SliderId")
  val posElem = elem(PositionId)
  val playButton = elem(PlayButton)
  val pauseButton = elem(PauseButton)
  val prevButton = elem(PrevButton)
  val nextButton = elem(NextButton)
  val volumeButton = elem(VolumeButton)
  val titleElem = elem(TitleId)
  val noTrackElem = elem(NoTrackTextId)
  val albumElem = elem(AlbumId)
  val artistElem = elem(ArtistId)
  val playlistElem = elem(PlaylistId)
  val playlistEmptyElem = elem(EmptyPlaylistText)
  val volumeElem = MyJQuery(s"#$VolumeId")

  val zero = 0.seconds

  var currentPlaylist: Seq[TrackMeta] = Nil
  var isMute: Boolean = false

  installHandlers()

  import JQueryUI.jQueryExtensions

  private def installHandlers(): Unit =
    prevButton.onClick(_ => send(PrevMsg))
    nextButton.onClick(_ => send(NextMsg))
    playButton.onClick(_ => send(ResumeMsg))
    pauseButton.onClick(_ => send(StopMsg))
    volumeButton.onClick(_ => toggleMute())
    val seekOptions = StopOptions.default((_, ui) => send(Playback.seek(ui.value)))
    MyJQuery(s"#$SliderId").slider(seekOptions)
//    sliderElem.slider(seekOptions)
    val volumeOptions = SliderOptions.horizontal(Min, 0, 100): ui =>
      send(Playback.volume(ui.value))
    volumeElem.slider(volumeOptions)

  private def toggleMute() =
    isMute = !isMute
    send(ValuedCommand.mute(isMute))

  override def muteToggled(mute: Boolean): Unit =
    isMute = mute

  def onStatus(status: StatusEvent): Unit =
    showConnected()

    val track = status.track
    if track.title.nonEmpty then updateTrack(track)
    posElem.html(format(status.position))
    updateVolume(status.volume)
    isMute = status.mute
    updateTimeAndDuration(status.position, track.duration)
    updatePlaylist(status.playlist)
    updatePlayPauseButtons(status.state)

    playerDiv.show()

  def updateVolume(vol: Volume): Unit =
    volumeElem.slider(OptionKey, Value, vol.volume)

  private def updateTimeAndDuration(position: Duration, duration: Duration) =
    updateDuration(duration)
    updateTime(position)

  def updateTime(position: Duration): Unit =
    posElem.html(format(position))
    sliderElem.slider(OptionKey, Value, position.toSeconds)

  private def updateDuration(duration: Duration) =
    durationElem.html(format(duration))
    sliderElem.slider(OptionKey, Max, duration.toSeconds)

  def updatePlayPauseButtons(state: PlayState): Unit =
    if state == Started then
      playButton.hide()
      pauseButton.show()
    else
      pauseButton.hide()
      playButton.show()

  def updateTrack(track: TrackMeta): Unit =
    titleElem.html(track.title)
    noTrackElem.hide()
    albumElem.html(track.album)
    artistElem.html(track.artist)
    updateTimeAndDuration(zero, track.duration)

  def updatePlaylist(tracks: Seq[TrackMeta]): Unit =
    playlistElem.getElementsByClassName(SongClass).foreach(e => e.remove())
    val isEmpty = tracks.isEmpty
    if isEmpty then playlistEmptyElem.show()
    else playlistEmptyElem.hide()
    tracks.zipWithIndex foreach { case (t, index) =>
      val rowId = s"playlist-$index"
      playlistElem.append(toRow(t, rowId).render)
      elem(rowId).onClick(_ => send(Playback.skip(index)))
    }

  def toRow(track: TrackMeta, rowId: String) =
    li(`class` := SongClass)(
      a(href := "#", id := rowId)(track.title),
      " ",
      a(href := "#")(i(`class` := "icon-remove"))
    )

  def format(time: Duration) = Playback.toHHMMSS(time)
