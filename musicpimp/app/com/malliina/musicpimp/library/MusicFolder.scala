package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.db.DataFolder
import com.malliina.http.FullUrl
import com.malliina.play.http.FullUrls
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Codec, Encoder}
import play.api.mvc.RequestHeader

case class MusicFolderResult(folder: FullFolder, folders: Seq[FullFolder], tracks: Seq[FullTrack])
  derives Codec.AsObject

case class MusicFolder(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]):
  val isEmpty = folders.isEmpty && tracks.isEmpty

  def toFull(host: FullUrl) =
    MusicFolderResult(
      folder.toFull(host),
      folders.map(_.toFull(host)),
      tracks.map(t => TrackJson.toFull(t, host))
    )

object MusicFolder:
  val empty = MusicFolder(DataFolder.root, Nil, Nil)

  def writer(request: RequestHeader): Encoder[MusicFolder] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Encoder[MusicFolder] =
    implicit val f: Encoder[FolderMeta] = FolderMeta.writer(host)
    implicit val t: Codec[TrackMeta] = TrackJson.format(host)
    deriveEncoder[MusicFolder]
