package com.malliina.musicpimp.json

object PimpStrings extends PimpStrings

trait PimpStrings extends PlaylistKeys with TrackKeys {
  val AlarmsAdd = "alarms_add"
  val AlarmsEdit = "alarms_edit"
  val AlarmsKey = "alarms"
  val AuthenticateKey = "authenticate"
  val Beam = "beam"
  val Body = "body"
  val Cmd = "cmd"
  val FolderKey = "folder"
  val Id = "id"
  val Limit = "limit"
  val Meta = "meta"
  val PasswordKey = "password"
  val Ping = "ping"
  val PingAuthKey = "ping_auth"
  val Player = "player"
  val Popular = "popular"
  val Range = "range"
  val Reason = "reason"
  val Recent = "recent"
  val RootFolderKey = "root_folder"
  val SearchKey = "search"
  val StatusKey = "status"
  val Term = "term"
  val TrackKey = "track"
  val UsernameKey = "username"
  val VersionKey = "version"
}

trait PlaylistKeys {
  val PlaylistDelete = "playlist_delete"
  val PlaylistGet = "playlist"
  val PlaylistKey = "playlist"
  val PlaylistSave = "playlist_save"
  val PlaylistsGet = "playlists"
  val PlaylistsKey = "playlists"
}

object TrackKeys extends TrackKeys

trait TrackKeys {
  val Album = "album"
  val Artist = "artist"
  val DurationKey = "duration"
  val PathKey = "path"
  val Size = "size"
  val Title = "title"
  val Url = "url"
}
