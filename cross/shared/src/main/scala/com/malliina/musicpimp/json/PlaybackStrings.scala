package com.malliina.musicpimp.json

object PlaybackStrings extends PlaybackStrings

trait PlaybackStrings:
  val Add = "add"
  val AddItemsKey = "add_items"
  val Mute = "mute"
  val Next = "next"
  val Play = "play"
  val PlayItemsKey = "play_items"
  val Prev = "prev"
  val Resume = "resume"
  val Seek = "seek"
  val Skip = "skip"
  val Status = "status"
  val Stop = "stop"
  val TrackKey = "track"
  val VolumeKey = "volume"

  // Events
  val Welcome = "welcome"
  val MuteToggled = "mute_toggled"
  val TimeUpdated = "time_updated"
  val TrackChanged = "track_changed"
  val PlaylistIndexChanged = "playlist_index_changed"
  val PlaylistModified = "playlist_modified"
  val PlaystateChanged = "playstate_changed"
  val VolumeChanged = "volume_changed"

  val PlaylistIndexv17v18 = "playlist_index"
  val PlaylistIndex = "index"

  val Remove = "remove"
  val Insert = "insert"
  val ResetPlaylist = "reset_playlist"
  val Move = "move"
  val From = "from"
  val To = "to"

  val Value = "value"

  val Index = "index"
  val Tracks = "tracks"
  val Folders = "folders"
