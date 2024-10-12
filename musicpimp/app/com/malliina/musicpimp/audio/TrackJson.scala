package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.models.*
import com.malliina.play.http.FullUrls
import io.circe.{Codec, Encoder}
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

import scala.concurrent.duration.Duration

object TrackJson:
  val reverse = controllers.musicpimp.routes.LibraryController
  implicit val dur: Codec[Duration] = PrimitiveFormats.durationCodec

  def urlFor(host: FullUrl, track: TrackID): FullUrl =
    FullUrls.absolute(host, reverse.download(track))

  def writer(request: RequestHeader): Encoder[TrackMeta] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Encoder[TrackMeta] = TrackMetas.writer(
    host,
    id => reverse.download(id)
  )

  def format(request: RequestHeader): Codec[TrackMeta] =
    format(host(request))

  def host(rh: RequestHeader) = FullUrls.hostOnly(rh)

  def format(host: FullUrl): Codec[TrackMeta] =
    Codec.from(TrackMeta.reader, writer(host))

  def makeFull(t: TrackMeta, rh: RequestHeader) = toFull(t, host(rh))

  def toFull(t: TrackMeta, host: FullUrl): FullTrack = t.toFull(urlFor(host, t.id))

  def toFullPlaylist(t: SavedPlaylist, host: FullUrl): FullSavedPlaylist =
    FullSavedPlaylist(t.id, t.name, t.trackCount, t.duration, t.tracks.map(toFull(_, host)))

  def toFullPlaylistsMeta(t: PlaylistsMeta, host: FullUrl) =
    FullSavedPlaylistsMeta(t.playlists.map(toFullPlaylist(_, host)))

  def toFullMeta(p: PlaylistMeta, host: FullUrl): FullPlaylistMeta =
    FullPlaylistMeta(toFullPlaylist(p.playlist, host))
