package com.malliina.musicpimp.json

object PlaybackStrings extends PlaybackStrings

trait PlaybackStrings {
  val Add = "add"
  val AddItems = "add_items"
  val Mute = "mute"
  val Next = "next"
  val Play = "play"
  val PlayItems = "play_items"
  val Prev = "prev"
  val Resume = "resume"
  val Seek = "seek"
  val Skip = "skip"
  val Status = "status"
  val Stop = "stop"
  val Volume = "volume"

  // Events
  val MuteToggled = "mute_toggled"
  val TimeUpdated = "time_updated"
  val TrackChanged = "track_changed"
  val PlaylistIndexChanged = "playlist_index_changed"
  val PlaylistModified = "playlist_modified"
  val PlaystateChanged = "playstate_changed"
  val VolumeChanged = "volume_changed"
}
