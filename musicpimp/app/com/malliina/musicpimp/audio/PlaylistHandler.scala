package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats.{cmd, singleCmd}
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.PlaylistID
import play.api.libs.json._

trait PlaylistCommand

case object GetPlaylistsCommand extends PlaylistCommand {
  val Key = "playlists"
  implicit val json = singleCmd(Key, GetPlaylistsCommand)
}

case class GetPlaylistCommand(id: PlaylistID) extends PlaylistCommand

object GetPlaylistCommand {
  val Key = "playlist"
  implicit val json = cmd(Key, Json.format[GetPlaylistCommand])
}

case class SavePlaylistCommand(playlist: PlaylistSubmission) extends PlaylistCommand

object SavePlaylistCommand {
  val Key = "playlist_save"
  implicit val json = cmd(Key, Json.format[SavePlaylistCommand])
}

case class DeletePlaylistCommand(id: PlaylistID) extends PlaylistCommand

object DeletePlaylistCommand {
  val Key = "playlist_delete"
  implicit val json = cmd(Key, Json.format[DeletePlaylistCommand])
}

object PlaylistCommand {
  implicit val reader: Reads[PlaylistCommand] = Reads { json =>
    GetPlaylistsCommand.json.reads(json)
      .orElse(GetPlaylistCommand.json.reads(json))
      .orElse(SavePlaylistCommand.json.reads(json))
      .orElse(DeletePlaylistCommand.json.reads(json))
  }
}

class PlaylistHandler(service: PlaylistService)
