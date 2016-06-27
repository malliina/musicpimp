package com.malliina.musicpimp.json

object JsonStrings extends JsonStrings

trait JsonStrings {
  val Ok = "ok"
  val Ping = "ping"
  val PingAuthKey = "ping_auth"
  val Failure = "failure"
  val Status = "status"
  val Version = "version"
  val State = "state"
  val Reason = "reason"
  val AccessDenied = "Access denied"
  val InvalidParameter = "Invalid parameter"
  val InvalidCredentials = "Invalid credentials"
  val InvalidJson = "Invalid JSON"
  val NoFileInMultipart = "No file in multipart/form-data"
  val DatabaseError = "A database error occurred"
  val GenericError = "A generic error occurred"
  val FolderKey = "folder"
  val RootFolderKey = "root_folder"
  val Folders = "folders"
  val PathKey = "path"
  val Parent = "parent"
  val Tracks = "tracks"
  val Id = "id"
  val Contents = "contents"
  val Title = "title"
  val Album = "album"
  val Artist = "artist"
  val Size = "size"
  val Gain = "gain"
  val Mute = "mute"
  val IsDir = "is_dir"
  val Items = "items"
  val Player = "player"
  val Playlist = "playlist"
  val PlaylistIndexv17v18 = "playlist_index"
  val PlaylistIndex = "index"
  //  @deprecated("Use POSITION", "1.8.0")
  val Pos = "pos"
  val Position = "position"
  //  @deprecated("Use POSITION", "1.8.0")
  val PosSeconds = "pos_seconds"
  val DurationKey = "duration"
  //  @deprecated("Use DURATION", "1.8.0")
  val DurationSeconds = "duration_seconds"
  val Msg = "msg"
  val ThankYou = "thank you"

  val Cmd = "cmd"
  val Value = "value"
  val TrackKey = "track"
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

  val AuthenticateKey = "authenticate"
  val Username = "username"
  val Password = "password"

  val Event = "event"
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

  //  val PLAYING = "playing"
  //  val PAUSED = "paused"
  //  val TIMEUPDATE = "timeupdate"
  //  val SKIPPED = "skipped"

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
  //  val HOUR = "hour"
  //  val MINUTE = "minute"
  //  val DAYS = "days"

  // Search
  val SearchKey = "search"
  val Term = "term"
  val Limit = "limit"
  val Refresh = "refresh"
  val SearchStatus = "search_status"

  val AlarmsKey = "alarms"
  val AlarmsEdit = "alarms_edit"
  val AlarmsAdd = "alarms_add"

  val Meta = "meta"
  val Beam = "beam"
  val Range = "range"

  val PlaylistsGet = "playlists"
  val PlaylistGet = "playlist"
  val PlaylistSave = "playlist_save"
  val PlaylistDelete = "playlist_delete"
  val PlaylistKey = "playlist"
  val PlaylistsKey = "playlists"

  // push notifications
  val Push = "push"
  val Result = "result"
  val Body = "body"

  val Url = "url"

  val Recent = "recent"
  val Popular = "popular"
}
