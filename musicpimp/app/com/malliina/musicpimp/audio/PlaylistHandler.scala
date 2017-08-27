package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats.{cmd, singleCmd}
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.PlaylistID
import play.api.libs.json._

trait PlaylistCommand

case object GetPlaylists extends PlaylistCommand {
  val Key = "playlists"
  implicit val json = singleCmd(Key, GetPlaylists)
}

case class GetPlaylist(id: PlaylistID) extends PlaylistCommand

object GetPlaylist {
  val Key = "playlist"
  implicit val json = cmd(Key, Json.format[GetPlaylist])
}

case class SavePlaylist(playlist: PlaylistSubmission) extends PlaylistCommand

object SavePlaylist {
  val Key = "playlist_save"
  implicit val json = cmd(Key, Json.format[SavePlaylist])
}

case class DeletePlaylist(id: PlaylistID) extends PlaylistCommand

object DeletePlaylist {
  val Key = "playlist_delete"
  implicit val json = cmd(Key, Json.format[DeletePlaylist])
}

object PlaylistCommand {
  implicit val reader: Reads[PlaylistCommand] = Reads { json =>
    GetPlaylists.json.reads(json)
      .orElse(GetPlaylist.json.reads(json))
      .orElse(SavePlaylist.json.reads(json))
      .orElse(DeletePlaylist.json.reads(json))
  }
}

class PlaylistHandler(service: PlaylistService)