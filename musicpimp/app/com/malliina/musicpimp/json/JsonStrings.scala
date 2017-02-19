package com.malliina.musicpimp.json

object JsonStrings extends JsonStrings

trait JsonStrings extends PimpStrings {
  val Ok = "ok"

  val Failure = "failure"
  val State = "state"
  val AccessDenied = "Access denied"
  val InvalidParameter = "Invalid parameter"
  val InvalidCredentials = "Invalid credentials"
  val InvalidJson = "Invalid JSON"
  val NoFileInMultipart = "No file in multipart/form-data"
  val DatabaseError = "A database error occurred"
  val GenericError = "A generic error occurred"

  val Folders = "folders"
  val Parent = "parent"
  val Tracks = "tracks"
  val Contents = "contents"

  val Gain = "gain"
  val Mute = "mute"
  val IsDir = "is_dir"
  val Items = "items"
  val Playlist = "playlist"
  val PlaylistIndexv17v18 = "playlist_index"
  val PlaylistIndex = "index"
  //  @deprecated("Use POSITION", "1.8.0")
  val Pos = "pos"
  val Position = "position"
  //  @deprecated("Use POSITION", "1.8.0")
  val PosSeconds = "pos_seconds"
  //  @deprecated("Use DURATION", "1.8.0")
  val DurationSeconds = "duration_seconds"
  val Msg = "msg"
  val ThankYou = "thank you"

  val Value = "value"
  val Resume = "resume"
  val Stop = "stop"
  val Next = "next"
  val Prev = "prev"
  val Volume = "volume"
  val Seek = "seek"
  val Skip = "skip"
  val Add = "add"
  val AddItems = "add_items"
  val Play = "play"
  val PlayItems = "play_items"
  val TrackChanged = "track_changed"
  val TimeUpdated = "time_updated"
  val VolumeChanged = "volume_changed"
  val MuteToggled = "mute_toggled"
  val PlaylistModified = "playlist_modified"
  val PlaylistIndexChanged = "playlist_index_changed"
  val PlaystateChanged = "playstate_changed"

  // commands for browser clients
  val WebPlay = "webplay"
  val WebAdd = "webadd"
  val WebStop = "webstop"
  val WebResume = "webresume"
  val WebNext = "webnext"
  val WebPrev = "webprev"
  val WebSkip = "webskip"
  val WebSeek = "webseek"
  val WebVolume = "webvolume"
  val WebMute = "webmute"
  val WebRemove = "webremove"
  val WebStatus = "webstatus"

  val Remove = "remove"
  val Insert = "insert"
  val ResetPlaylist = "reset_playlist"
  val Move = "move"
  val From = "from"
  val To = "to"

  val Subscribe = "subscribe"

  val TrackHeader = "Track"

  val Job = "job"
  val When = "when"
  val Enabled = "enabled"

  // Search
  val Refresh = "refresh"
  val SearchStatus = "search_status"

  // push notifications
  val Push = "push"
  val Result = "result"

}