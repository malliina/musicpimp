package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings
import org.musicpimp.js.PlayerState.Started
import org.scalajs.jquery.JQueryEventObject

import scala.concurrent.duration.{Duration, DurationInt}
import scalatags.Text.all._

case class Track(id: String,
                 title: String,
                 album: String,
                 artist: String,
                 duration: Duration)

case class Status(track: Track,
                  position: Duration,
                  volume: Int,
                  mute: Boolean,
                  playlist: Seq[Track],
                  state: PlayerState)

object Playback extends PlaybackStrings {
  val SocketUrl = "/ws/playback?f=json"

  val status = Command(Status)
  val next = Command(Next)
  val prev = Command(Prev)
  val stop = Command(Stop)
  val resume = Command(Resume)

  def volume(vol: Int) = ValuedCommand(Volume, vol)

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

class Playback extends PlaybackSocket {
  val OptionKey = "option"
  val Max = "max"
  val Min = "min"
  val Value = "value"
  val playerDiv = elem("playerDiv")
  val durationElem = elem("duration")
  val sliderElem = elem("slider")
  val sliderDyn = global.jQuery("#slider")
  val posElem = elem("pos")
  val playButton = elem("playButton")
  val pauseButton = elem("pauseButton")
  val prevButton = elem("prevButton")
  val nextButton = elem("nextButton")
  val volumeButton = elem("volumeButton")
  val titleElem = elem("title")
  val noTrackElem = elem("notracktext")
  val albumElem = elem("album")
  val artistElem = elem("artist")
  val playlistElem = elem("playlist")
  val playlistEmptyElem = elem("empty_playlist_text")
  val volumeElemDyn = global.jQuery("#volume")

  val zero = 0.seconds

  var currentPlaylist: Seq[Track] = Nil
  var isMute: Boolean = false

  installHandlers()

  private def installHandlers() = {
    prevButton.click((_: JQueryEventObject) => send(Playback.prev))
    nextButton.click((_: JQueryEventObject) => send(Playback.next))
    playButton.click((_: JQueryEventObject) => send(Playback.resume))
    pauseButton.click((_: JQueryEventObject) => send(Playback.stop))
    volumeButton.click((_: JQueryEventObject) => toggleMute())
    val seekOptions = StopOptions.default((_, ui) => send(Playback.seek(ui.value)))
    sliderDyn.slider(seekOptions)
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

  def onStatus(status: Status) = {
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

  def updateVolume(vol: Int) =
    volumeElemDyn.slider(OptionKey, Value, vol)

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
    sliderDyn.slider(OptionKey, Value, duration.toSeconds)
  }

  def updatePlayPauseButtons(state: PlayerState) = {
    if (state == Started) {
      playButton.hide()
      pauseButton.show()
    } else {
      pauseButton.hide()
      playButton.show()
    }
  }

  def updateTrack(track: Track) = {
    titleElem.html(track.title)
    noTrackElem.hide()
    albumElem.html(track.album)
    artistElem.html(track.artist)
    updateTimeAndDuration(zero, track.duration)
  }

  def updatePlaylist(tracks: Seq[Track]) = {
    global.jQuery("li").remove(".song")
    val isEmpty = tracks.isEmpty
    if (isEmpty) playlistEmptyElem.show()
    else playlistEmptyElem.hide()
    tracks.zipWithIndex foreach { case (t, index) =>
      val rowId = s"playlist-$index"
      playlistElem.append(toRow(t, rowId).toString())
      elem(rowId).click((_: JQueryEventObject) => send(Playback.skip(index)))
    }
  }

  def toRow(track: Track, rowId: String) = {
    li(`class` := "song")(
      a(href := "#", id := rowId)(track.title),
      " ",
      a(href := "#")(i(`class` := "icon-remove")))
  }

  def format(time: Duration) = Playback.toHHMMSS(time)
}
