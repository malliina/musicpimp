package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models._
import com.malliina.play.http.FullUrls
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

object TrackJson {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  def urlFor(host: FullUrl, track: TrackID): FullUrl =
    FullUrls.absolute(host, controllers.musicpimp.routes.LibraryController.supplyForPlayback(track))

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Writes[TrackMeta] = TrackMetas.writer(
    host,
    id => controllers.musicpimp.routes.LibraryController.supplyForPlayback(id)
  )

  def format(request: RequestHeader): Format[TrackMeta] =
    format(host(request))

  def host(rh: RequestHeader) = FullUrls.hostOnly(rh)

  def format(host: FullUrl): Format[TrackMeta] =
    Format(TrackMeta.reader, writer(host))

  def toFull(t: TrackMeta, host: FullUrl): FullTrack =
    FullTrack(t.id, t.title, t.artist, t.album, t.path, t.duration, t.size, urlFor(host, t.id))

  def toFullPlaylist(t: SavedPlaylist, host: FullUrl): FullSavedPlaylist =
    FullSavedPlaylist(t.id, t.name, t.tracks.map(toFull(_, host)))

  def toFullPlaylistsMeta(t: PlaylistsMeta, host: FullUrl) =
    FullSavedPlaylistsMeta(t.playlists.map(toFullPlaylist(_, host)))

  def toFullMeta(p: PlaylistMeta, host: FullUrl) =
    FullPlaylistMeta(toFullPlaylist(p.playlist, host))
}
