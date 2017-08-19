package com.malliina.ws

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.PimpStream
import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader, Result}

import scala.concurrent.Future

object Streamer {
  val DefaultMaxUploadSize = 1024.megs
}

trait Streamer {
  val maxUploadSize = Streamer.DefaultMaxUploadSize

  def snapshot: Seq[PimpStream]

  def exists(uuid: RequestID): Boolean

  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Result

  def parser(uuid: RequestID): Option[BodyParser[MultipartFormData[Long]]]

  /**
    * @param uuid        request ID
    * @param shouldAbort if true, the server is informed that it should cancel the request
    * @return true if `uuid` was found, false otherwise
    */
  def remove(uuid: RequestID, shouldAbort: Boolean, wasSuccess: Boolean): Future[Boolean]
}
