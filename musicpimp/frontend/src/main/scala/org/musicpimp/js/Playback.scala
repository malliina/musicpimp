package org.musicpimp.js

import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.js.PlayerStrings
import com.malliina.musicpimp.json.PlaybackStrings
import com.malliina.musicpimp.models.Volume
import org.scalajs.jquery.JQueryEventObject
import scalatags.Text.all._

import scala.concurrent.duration.{Duration, DurationInt}

object Playback extends PlaybackStrings {
  val SocketUrl = "/ws/playback?f=json"

  def volume(vol: Int) = ValuedCommand(VolumeKey, vol)

  def seek(pos: Int) = ValuedCommand(Seek, pos)

  def skip(idx: Int) = ValuedCommand(Skip, idx)

  /**
    *
    * @param duration a duration of time
    * @return "HH:mm:ss" if `duration` >= 1 hour, otherwise "mm:ss"
    */
  def toHHMMSS(duration: Duration) = {
    val s = duration.toSeconds
    val hours = duration.toHours
    if (hours > 0) "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    else "%02d:%02d".format((s % 3600) / 60, s % 60)
  }
}

class Playback extends PlaybackSocket with PlayerStrings {
  val OptionKey = "option"
  val Max = "max"
  val Min = "min"
  val SongClass = "song"
  val playerDiv = elem(PlayerDivId)
  val durationElem = elem(DurationId)
  val sliderElem = elem(SliderId)
  val sliderDyn = MyJQuery(s"#$SliderId")
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
  val volumeElemDyn = MyJQuery(s"#$VolumeId")

  val zero = 0.seconds

  var currentPlaylist: Seq[TrackMeta] = Nil
  var isMute: Boolean = false

  installHandlers()

  import JQueryUI.jQueryExtensions

  private def installHandlers() = {
    prevButton.click((_: JQueryEventObject) => send(PrevMsg))
    nextButton.click((_: JQueryEventObject) => send(NextMsg))
    playButton.click((_: JQueryEventObject) => send(ResumeMsg))
    pauseButton.click((_: JQueryEventObject) => send(StopMsg))
    volumeButton.click((_: JQueryEventObject) => toggleMute())
    val seekOptions = StopOptions.default((_, ui) => send(Playback.seek(ui.value)))
    sliderElem.slider(seekOptions)
    val volumeOptions = SliderOptions.horizontal(Min, 0, 100) { ui =>
      send(Playback.volume(ui.value))
    }
    volumeElemDyn.slider(volumeOptions)
  }

  def toggleMute() = {
    isMute = !isMute
    send(ValuedCommand.mute(isMute))
  }

  override def muteToggled(mute: Boolean) = {
    isMute = mute
  }

  def onStatus(status: StatusEvent) = {
    showConnected()

    val track = status.track
    if (track.title.nonEmpty) {
      updateTrack(track)
    }
    posElem.html(format(status.position))
    updateVolume(status.volume)
    isMute = status.mute
    updateTimeAndDuration(status.position, track.duration)
    updatePlaylist(status.playlist)
    updatePlayPauseButtons(status.state)

    playerDiv.show()
  }

  def updateVolume(vol: Volume) =
    volumeElemDyn.slider(OptionKey, Value, vol.volume)

  def updateTimeAndDuration(position: Duration, duration: Duration) = {
    updateDuration(duration)
    updateTime(position)
  }

  def updateTime(position: Duration) = {
    posElem.html(format(position))
    sliderDyn.slider(OptionKey, Value, position.toSeconds)
  }

  def updateDuration(duration: Duration) = {
    durationElem.html(format(duration))
    sliderDyn.slider(OptionKey, Max, duration.toSeconds)
  }

  def updatePlayPauseButtons(state: PlayState) = {
    if (state == Started) {
      playButton.hide()
      pauseButton.show()
    } else {
      pauseButton.hide()
      playButton.show()
    }
  }

  def updateTrack(track: TrackMeta) = {
    titleElem.html(track.title)
    noTrackElem.hide()
    albumElem.html(track.album)
    artistElem.html(track.artist)
    updateTimeAndDuration(zero, track.duration)
  }

  def updatePlaylist(tracks: Seq[TrackMeta]) = {
    MyJQuery("li").remove(s".$SongClass")
    val isEmpty = tracks.isEmpty
    if (isEmpty) playlistEmptyElem.show()
    else playlistEmptyElem.hide()
    tracks.zipWithIndex foreach { case (t, index) =>
      val rowId = s"playlist-$index"
      playlistElem.append(toRow(t, rowId).toString())
      elem(rowId).click((_: JQueryEventObject) => send(Playback.skip(index)))
    }
  }

  def toRow(track: TrackMeta, rowId: String) = {
    li(`class` := SongClass)(
      a(href := "#", id := rowId)(track.title),
      " ",
      a(href := "#")(i(`class` := "icon-remove")))
  }

  def format(time: Duration) = Playback.toHHMMSS(time)
}
