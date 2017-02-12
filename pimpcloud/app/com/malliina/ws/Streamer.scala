package com.malliina.ws

import java.util.UUID

import com.malliina.musicpimp.audio.Track
import com.malliina.pimpcloud.ws.StreamData
import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader, Result}

import scala.concurrent.Future

object Streamer {
  val DefaultMaxUploadSize = 1024.megs
}

trait Streamer {
  val maxUploadSize = Streamer.DefaultMaxUploadSize

  def snapshot: Seq[StreamData]

  def exists(uuid: UUID): Boolean

  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Future[Option[Result]]

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]]

  /**
    * @param uuid        request ID
    * @param shouldAbort if true, the server is informed that it should cancel the request
    * @return true if `uuid` was found, false otherwise
    */
  def remove(uuid: UUID, shouldAbort: Boolean, wasSuccess: Boolean): Future[Boolean]
}
