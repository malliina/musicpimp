package com.malliina.ws

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.PimpStream
import com.malliina.pimpcloud.streams.StreamEndpoint
import com.malliina.play.ContentRange
import com.malliina.storage.{StorageInt, StorageSize}
import org.http4s.{Request, Response}

object Streamer:
  private val DefaultMaxUploadSize: StorageSize = 1024.megs

trait Streamer[F[_]]:
  val maxUploadSize = Streamer.DefaultMaxUploadSize

  def find(request: RequestID): Option[StreamEndpoint[F]]
  def snapshot: Seq[PimpStream]
  def exists(uuid: RequestID): Boolean
  def requestTrack(track: Track, range: ContentRange, req: Request[?]): F[Response[F]]

  /** @param uuid
    *   request ID
    * @param shouldAbort
    *   if true, the server is informed that it should cancel the request
    * @return
    *   true if `uuid` was found, false otherwise
    */
  def remove(uuid: RequestID, shouldAbort: Boolean, wasSuccess: Boolean): F[Boolean]
