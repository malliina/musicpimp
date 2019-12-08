package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.models._
import com.malliina.play.http.FullUrls
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

import scala.concurrent.duration.Duration

object TrackJson {
  val reverse = controllers.musicpimp.routes.LibraryController
  implicit val dur: Format[Duration] = PrimitiveFormats.durationFormat

  def urlFor(host: FullUrl, track: TrackID): FullUrl =
    FullUrls.absolute(host, reverse.download(track))

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Writes[TrackMeta] = TrackMetas.writer(
    host,
    id => reverse.download(id)
  )

  def format(request: RequestHeader): Format[TrackMeta] =
    format(host(request))

  def host(rh: RequestHeader) = FullUrls.hostOnly(rh)

  def format(host: FullUrl): Format[TrackMeta] =
    Format(TrackMeta.reader, writer(host))

  def makeFull(t: TrackMeta, rh: RequestHeader) = toFull(t, host(rh))

  def toFull(t: TrackMeta, host: FullUrl): FullTrack = t.toFull(urlFor(host, t.id))

  def toFullPlaylist(t: SavedPlaylist, host: FullUrl): FullSavedPlaylist =
    FullSavedPlaylist(t.id, t.name, t.trackCount, t.duration, t.tracks.map(toFull(_, host)))

  def toFullPlaylistsMeta(t: PlaylistsMeta, host: FullUrl) =
    FullSavedPlaylistsMeta(t.playlists.map(toFullPlaylist(_, host)))

  def toFullMeta(p: PlaylistMeta, host: FullUrl): FullPlaylistMeta =
    FullPlaylistMeta(toFullPlaylist(p.playlist, host))
}
