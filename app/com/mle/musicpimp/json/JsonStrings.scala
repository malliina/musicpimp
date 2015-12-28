package com.mle.musicpimp.json

/**
 * @author Michael
 */
trait JsonStrings {
  val OK = "ok"
  val PING = "ping"
  val PING_AUTH = "ping_auth"
  val FAILURE = "failure"
  val STATUS = "status"
  val VERSION = "version"
  val STATE = "state"
  val REASON = "reason"
  val ACCESS_DENIED = "Access denied"
  val INVALID_PARAMETER = "Invalid parameter"
  val INVALID_CREDENTIALS = "Invalid credentials"
  val INVALID_JSON = "Invalid JSON"
  val NO_FILE_IN_MULTIPART = "No file in multipart/form-data"
  val DatabaseError = "A database error occurred"
  val GenericError = "A generic error occurred"
  val FOLDER = "folder"
  val ROOT_FOLDER = "root_folder"
  val FOLDERS = "folders"
  val PATH = "path"
  val PARENT = "parent"
  val TRACKS = "tracks"
  val ID = "id"
  val CONTENTS = "contents"
  val TITLE = "title"
  val ALBUM = "album"
  val ARTIST = "artist"
  val SIZE = "size"
  val GAIN = "gain"
  val MUTE = "mute"
  val IS_DIR = "is_dir"
  val ITEMS = "items"
  val PLAYER = "player"
  val PLAYLIST = "playlist"
  val PLAYLIST_INDEXv17v18 = "playlist_index"
  val PLAYLIST_INDEX = "index"
  //  @deprecated("Use POSITION", "1.8.0")
  val POS = "pos"
  val POSITION = "position"
  //  @deprecated("Use POSITION", "1.8.0")
  val POS_SECONDS = "pos_seconds"
  val DURATION = "duration"
  //  @deprecated("Use DURATION", "1.8.0")
  val DURATION_SECONDS = "duration_seconds"
  val MSG = "msg"
  val THANK_YOU = "thank you"

  val CMD = "cmd"
  val VALUE = "value"
  val TRACK = "track"
  val RESUME = "resume"
  val STOP = "stop"
  val NEXT = "next"
  val PREV = "prev"
  val VOLUME = "volume"
  val SEEK = "seek"
  val SKIP = "skip"
  val ADD = "add"
  val ADD_ITEMS = "add_items"
  val PLAY = "play"
  val PLAY_ITEMS = "play_items"

  val AUTHENTICATE = "authenticate"
  val USERNAME = "username"
  val PASSWORD = "password"

  val EVENT = "event"
  val TRACK_CHANGED = "track_changed"
  val TIME_UPDATED = "time_updated"
  val VOLUME_CHANGED = "volume_changed"
  val MUTE_TOGGLED = "mute_toggled"
  val PLAYLIST_MODIFIED = "playlist_modified"
  val PLAYLIST_INDEX_CHANGED = "playlist_index_changed"
  val PLAYSTATE_CHANGED = "playstate_changed"

  // commands for browser clients
  val WEBPLAY = "webplay"
  val WEBADD = "webadd"
  val WEBSTOP = "webstop"
  val WEBRESUME = "webresume"
  val WEBNEXT = "webnext"
  val WEBPREV = "webprev"
  val WEBSKIP = "webskip"
  val WEBSEEK = "webseek"
  val WEBVOLUME = "webvolume"
  val WEBMUTE = "webmute"
  val WEBREMOVE = "webremove"
  val WEBSTATUS = "webstatus"

  //  val PLAYING = "playing"
  //  val PAUSED = "paused"
  //  val TIMEUPDATE = "timeupdate"
  //  val SKIPPED = "skipped"

  val REMOVE = "remove"
  val Insert = "insert"

  val SUBSCRIBE = "subscribe"

  val TRACK_HEADER = "Track"

  val JOB = "job"
  val WHEN = "when"
  val ENABLED = "enabled"
  //  val HOUR = "hour"
  //  val MINUTE = "minute"
  //  val DAYS = "days"

  // Search
  val SEARCH = "search"
  val TERM = "term"
  val LIMIT = "limit"
  val REFRESH = "refresh"
  val SEARCH_STATUS = "search_status"

  val ALARMS = "alarms"
  val ALARMS_EDIT = "alarms_edit"
  val ALARMS_ADD = "alarms_add"

  val META = "meta"
  val BEAM = "beam"
  val RANGE = "range"

  val PlaylistsGet = "playlists"
  val PlaylistGet = "playlist"
  val PlaylistSave = "playlist_save"
  val PlaylistDelete = "playlist_delete"
  val PlaylistKey = "playlist"
  val PlaylistsKey = "playlists"
}

object JsonStrings extends JsonStrings
