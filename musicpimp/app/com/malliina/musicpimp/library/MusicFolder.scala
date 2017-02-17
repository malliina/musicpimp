package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.{FolderMeta, TrackJson, TrackMeta}
import com.malliina.musicpimp.db.DataFolder
import com.malliina.musicpimp.models.PimpUrl
import play.api.libs.json.{Json, Writes}
import play.api.mvc.RequestHeader

case class MusicFolder(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object MusicFolder {
  val empty = MusicFolder(DataFolder.root, Nil, Nil)

  def writer(request: RequestHeader): Writes[MusicFolder] =
    writer(PimpUrl.hostOnly(request))

  def writer(host: PimpUrl): Writes[MusicFolder] = {
    implicit val f = FolderMeta.writer(host)
    implicit val t = TrackJson.format(host)
    Json.writes[MusicFolder]
  }
}
