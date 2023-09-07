package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.db.DataFolder
import com.malliina.http.FullUrl
import com.malliina.play.http.FullUrls
import play.api.libs.json.{Json, OFormat, Writes}
import play.api.mvc.RequestHeader

case class MusicFolderResult(folder: FullFolder, folders: Seq[FullFolder], tracks: Seq[FullTrack])

object MusicFolderResult {
  implicit val json: OFormat[MusicFolderResult] = Json.format[MusicFolderResult]
}

case class MusicFolder(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty

  def toFull(host: FullUrl) =
    MusicFolderResult(
      folder.toFull(host),
      folders.map(_.toFull(host)),
      tracks.map(t => TrackJson.toFull(t, host))
    )
}

object MusicFolder {
  val empty = MusicFolder(DataFolder.root, Nil, Nil)

  def writer(request: RequestHeader): Writes[MusicFolder] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Writes[MusicFolder] = {
    implicit val f = FolderMeta.writer(host)
    implicit val t = TrackJson.format(host)
    Json.writes[MusicFolder]
  }
}
