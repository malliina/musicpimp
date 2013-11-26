package com.mle.musicpimp.json

/**
 * @author Michael
 */
trait JsonStrings {
  val OK = "ok"
  val FAILURE = "failure"
  val STATUS = "status"
  val VERSION = "version"
  val STATE = "state"
  val REASON = "reason"
  val ACCESS_DENIED = "Access denied"
  val INVALID_PARAMETER = "Invalid parameter"
  val INVALID_JSON = "Invalid JSON"
  val NO_FILE_IN_MULTIPART = "No file in multipart/form-data"
  val FOLDER = "folder"
  val FOLDERS = "folders"
  val PATH = "path"
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
  val PLAYLIST = "playlist"
  val PLAYLIST_INDEX = "playlist_index"
  @deprecated("Use POSITION", "1.8.0")
  val POS = "pos"
  val POSITION = "position"
  @deprecated("Use POSITION", "1.8.0")
  val POS_SECONDS = "pos_seconds"
  val DURATION = "duration"
  @deprecated("Use DURATION", "1.8.0")
  val DURATION_SECONDS = "duration_seconds"

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
  val PLAY = "play"

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
}

object JsonStrings extends JsonStrings
