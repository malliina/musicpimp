package com.malliina.musicpimp.audio

import cats.effect.IO
import com.malliina.musicpimp.json.CrossFormats.{cmd, singleCmd}
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.PlaylistID
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder}

trait PlaylistCommand

case object GetPlaylistsCommand extends PlaylistCommand:
  val Key = "playlists"
  implicit val json: Codec[GetPlaylistsCommand.type] = singleCmd(Key, GetPlaylistsCommand)

case class GetPlaylistCommand(id: PlaylistID) extends PlaylistCommand

object GetPlaylistCommand:
  val Key = "playlist"
  implicit val json: Codec[GetPlaylistCommand] = cmd(Key, deriveCodec[GetPlaylistCommand])

case class SavePlaylistCommand(playlist: PlaylistSubmission) extends PlaylistCommand

object SavePlaylistCommand:
  val Key = "playlist_save"
  implicit val json: Codec[SavePlaylistCommand] = cmd(Key, deriveCodec[SavePlaylistCommand])

case class DeletePlaylistCommand(id: PlaylistID) extends PlaylistCommand

object DeletePlaylistCommand:
  val Key = "playlist_delete"
  implicit val json: Codec[DeletePlaylistCommand] = cmd(Key, deriveCodec[DeletePlaylistCommand])

object PlaylistCommand:
  implicit val reader: Decoder[PlaylistCommand] = Decoder: json =>
    val v = json.value
    GetPlaylistsCommand.json
      .decodeJson(v)
      .orElse(GetPlaylistCommand.json.decodeJson(v))
      .orElse(SavePlaylistCommand.json.decodeJson(v))
      .orElse(DeletePlaylistCommand.json.decodeJson(v))

class PlaylistHandler(service: PlaylistService[IO])
