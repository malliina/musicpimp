package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.http4s.Reverse
import com.malliina.musicpimp.models.*
import com.malliina.play.http.FullUrls2
import io.circe.{Codec, Encoder}

import scala.concurrent.duration.Duration

object TrackJson:
  val reverse = Reverse
  given dur: Codec[Duration] = PrimitiveFormats.durationCodec

  def urlFor(host: FullUrl, track: TrackID): FullUrl =
    FullUrls2.absolute(host, reverse.downloads(track))

  def writer(host: FullUrl): Encoder[TrackMeta] = TrackMetas.writer(
    host,
    id => reverse.downloads(id)
  )

  def format(host: FullUrl): Codec[TrackMeta] =
    Codec.from(TrackMeta.reader, writer(host))

  def toFull(t: TrackMeta, host: FullUrl): FullTrack = t.toFull(urlFor(host, t.id))

  private def toFullPlaylist(t: SavedPlaylist, host: FullUrl): FullSavedPlaylist =
    FullSavedPlaylist(t.id, t.name, t.trackCount, t.duration, t.tracks.map(toFull(_, host)))

  def toFullPlaylistsMeta(t: PlaylistsMeta, host: FullUrl) =
    FullSavedPlaylistsMeta(t.playlists.map(toFullPlaylist(_, host)))

  def toFullMeta(p: PlaylistMeta, host: FullUrl): FullPlaylistMeta =
    FullPlaylistMeta(toFullPlaylist(p.playlist, host))
