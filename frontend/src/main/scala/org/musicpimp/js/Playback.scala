package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject
import upickle.{Invalid, Js}

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
                  state: String)

object Playback {
  val status = Command("status")
  val next = Command("next")
  val prev = Command("prev")
  val stop = Command("stop")
  val resume = Command("resume")

  def volume(vol: Int) = ValuedCommand("volume", vol)

  def seek(pos: Int) = ValuedCommand("seek", pos)

  def skip(idx: Int) = ValuedCommand("skip", idx)

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

class Playback extends SocketJS("/ws/playback?f=json") {
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
    val volumeOptions = SliderOptions.horizontal("min", 0, 100) { ui =>
      send(Playback.volume(ui.value))
    }
    volumeElemDyn.slider(volumeOptions)
  }

  def toggleMute() = {
    isMute = !isMute
    send(ValuedCommand.mute(isMute))
  }

  override def handlePayload(payload: Js.Value) = withFailure {
    val obj = payload.obj

    def read[T: PimpJSON.Reader](key: String) = obj.get(key)
      .map(json => PimpJSON.readJs[T](json))
      .getOrElse(throw Invalid.Data(payload, s"Missing key: '$key'."))

    read[String]("event") match {
      case "welcome" =>
        send(Playback.status)
      case "time_updated" =>
        updateTime(read[Duration]("position"))
      case "playstate_changed" =>
        updatePlayPauseButtons(read[String]("state"))
      case "track_changed" =>
        updateTrack(read[Track]("track"))
      case "playlist_modified" =>
        updatePlaylist(read[Seq[Track]]("playlist"))
      case "volume_changed" =>
        updateVolume(read[Int]("volume"))
      case "mute_toggled" =>
        isMute = read[Boolean]("mute")
      case "status" =>
        showConnected()
        onStatus(PimpJSON.readJs[Status](payload))
        playerDiv.show()
      case "playlist_index_changed" =>

      case other =>
        log.info(s"Unknown event: $other")
    }
  }

  def onStatus(status: Status) = {
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
  }

  def updateVolume(vol: Int) =
    volumeElemDyn.slider("option", "value", vol)

  def updateTimeAndDuration(position: Duration, duration: Duration) = {
    updateDuration(duration)
    updateTime(position)
  }

  def updateTime(position: Duration) = {
    posElem.html(format(position))
    sliderDyn.slider("option", "value", position.toSeconds)
  }

  def updateDuration(duration: Duration) = {
    durationElem.html(format(duration))
    sliderDyn.slider("option", "max", duration.toSeconds)
  }

  def updatePlayPauseButtons(state: String) = {
    if (state == "Started") {
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

  def withFailure(code: => Any) =
    try {
      code
    } catch {
      case i: Invalid => onInvalidData(i)
    }

  def format(time: Duration) = Playback.toHHMMSS(time)
}
