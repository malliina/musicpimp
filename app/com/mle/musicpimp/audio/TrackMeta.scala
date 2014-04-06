package com.mle.musicpimp.audio

import java.io.InputStream
import com.mle.musicpimp.json.JsonStrings._
import scala.concurrent.duration.Duration
import play.api.libs.json._
import play.api.libs.json.Json._
import com.mle.storage._

/**
 *
 * @author mle
 */
trait TrackMeta extends ITrackMeta {
  /**
   * Returns an [[InputStream]] of the media of the audio track.
   *
   * Calling this method multiple times may not be valid. This method is broken.
   *
   * The stream is either opened when this method is called, or it has already been opened. Users of the stream
   * know when it can be closed and should do so anyway. In other words, client code should treat the returned
   * stream as if it's opened when this method is called.
   *
   * @return an audio stream of the track
   */
  def stream: InputStream
}

object TrackMeta {
  implicit val trackWriter = new Writes[TrackMeta] {
    def writes(o: TrackMeta): JsValue = obj(
      ID -> o.id,
      TITLE -> o.title,
      ARTIST -> o.artist,
      ALBUM -> o.album,
      DURATION -> o.duration.toSeconds,
      SIZE -> o.size.toBytes
    )
  }

  case class StreamedTrack(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize, stream: InputStream) extends TrackMeta
}
