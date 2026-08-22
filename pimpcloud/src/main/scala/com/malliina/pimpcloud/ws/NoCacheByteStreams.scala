package com.malliina.pimpcloud.ws

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.auth.Proxies2
import com.malliina.musicpimp.cloud.{PimpServerSocket, UserRequest}
import com.malliina.musicpimp.json.PlaybackStrings.TrackKey
import com.malliina.musicpimp.json.SocketStrings.Cancel
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.{CloudID, RangedRequest, RequestID, WrappedID}
import com.malliina.pimpcloud.PimpStream
import com.malliina.pimpcloud.streams.{ChannelInfo, StreamEndpoint}
import com.malliina.pimpcloud.ws.NoCacheByteStreams.{DetachedMessage, log}
import com.malliina.play.ContentRange
import com.malliina.util.AppLogger
import com.malliina.ws.Streamer
import fs2.Stream
import fs2.concurrent.Topic
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.apache.pekko.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import org.apache.pekko.stream.{QueueOfferResult, StreamDetachedException}
import org.http4s.headers.Range.SubRange
import org.http4s.headers.{`Content-Length`, `Content-Range`, `Content-Type`}
import org.http4s.{MediaType, Request, Response, Status}
import org.typelevel.ci.CIStringSyntax
import play.api.mvc.*

import scala.collection.concurrent.TrieMap

object NoCacheByteStreams:
  private val log = AppLogger(getClass)

  // backpressures automatically, seems to work fine, and does not consume RAM
  val ByteStringBufferSize = 0
  val DetachedMessage = "Stream is terminated. SourceQueue is detached"

/** For each incoming request:
  *
  * 1) Assign an ID to the request 2) Open a channel (or create a promise) onto which we push the
  * eventual response 3) Forward the request along with its ID to the destination server 4) The
  * destination server tags its response with the request ID 5) Read the request ID from the
  * response and push the response to the channel (or complete the promise) 6) EOF and close the
  * channel; this completes the request-response cycle
  */
class NoCacheByteStreams[F[_]: Async](
  id: CloudID,
  val jsonOut: Target[F],
  onUpdate: F[Unit]
) extends Streamer[F]:
  val F = Async[F]
  private val ongoing = TrieMap.empty[RequestID, StreamEndpoint[F]]

  override def find(request: RequestID): Option[StreamEndpoint[F]] = get(request)

  def snapshot: Seq[PimpStream] = ongoing
    .map: (uuid, stream) =>
      PimpStream(uuid.toId, id, stream.track, stream.range)
    .toSeq

  /** @return
    *   a Result if the server received the upload request, None otherwise
    * @see
    *   https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def requestTrack(track: Track, range: ContentRange, req: Request[?]): F[Response[F]] =
    val request = RequestID.random()
    val userAgent = req.headers
      .get(ci"User-Agent")
      .map(ua => s"user agent ${ua.head.value}")
      .getOrElse("unknown user agent")
    Topic[F, Option[Seq[Byte]]].flatMap: topic =>
      val source: Stream[F, Byte] =
        topic.subscribe(100).takeWhile(_.isDefined).flatMap(bs => Stream.emits(bs.getOrElse(Nil)))
      ongoing += (request -> ChannelInfo(topic, id, track, range))
      onUpdate.flatMap: _ =>
        val address = Proxies2.realAddress(req)
        log.info(
          s"Created stream '$request' of track '${track.title}' with range '${range.description}' for '$userAgent' from '$address'."
        )
        connectSource(request, source, track, range)

  def exists(request: RequestID): Boolean = ongoing.contains(request)

  override def remove(
    request: RequestID,
    shouldAbort: Boolean,
    wasSuccess: Boolean
  ): F[Boolean] =
    val desc = if wasSuccess then "successful" else "failed"
    val description = s"$desc request '$request'"
    val disposal = disposeUUID(request)
      .map: fut =>
        fut
          .map: _ =>
            log.info(s"Removed $description")
          .recover:
            case ist: IllegalStateException if Option(ist.getMessage).contains(DetachedMessage) =>
              log.info(s"Removed $description after detachment")
//            case sde: StreamDetachedException =>
//              val msg = s"Removed $description after exceptional detachment"
//              if wasSuccess then log.debug(msg, sde) else log.warn(msg, sde)
            case e: Exception =>
              log.error(s"Removed but failed to close $description $e", e)
          .map(_ => true)
      .getOrElse:
        // This method is fired multiple times in normal circumstances
        log.debug(s"Unable to remove '$request'. Request ID not found.")
        F.pure(false)
    val send = if shouldAbort then sendMessage(cancelMessage(request)) else F.unit
    send >> disposal

  private def connectSource(
    request: RequestID,
    source: Stream[F, Byte],
    track: Track,
    range: ContentRange
  ): F[Response[F]] =
    val result =
      if range.isAll then
        Response(Status.Ok, body = source)
          .withContentType(`Content-Type`(MediaType.audio.mpeg))
          .putHeaders(`Content-Length`(range.size.bytes))
      else
        Response(Status.PartialContent, body = source)
          .withContentType(`Content-Type`(MediaType.audio.mpeg))
          .putHeaders(
            `Content-Length`(range.contentLength),
            `Content-Range`(SubRange(range.start, range.endInclusive), Option(range.size.toBytes))
          )
    connect(request, track, range).map: _ =>
      result

  private def connect(request: RequestID, track: Track, range: ContentRange): F[Unit] =
    val req = buildTrackRequest(request, track, range)
    sendMessage(req)

  /** Transfer complete.
    *
    * @param request
    *   the transfer ID
    */
  private def disposeUUID(request: RequestID): Option[F[Boolean]] =
    ongoing
      .remove(request)
      .map: e =>
        onUpdate >> e.close

  private def buildTrackRequest(request: RequestID, track: Track, range: ContentRange) =
    val requestJson =
      if range.isAll then WrappedID.forId(track.id).asJson
      else RangedRequest(track.id, range).asJson
    UserRequest(TrackKey, requestJson, request, PimpServerSocket.nobody)

  // Sends `msg` to the MusicPimp server
  private def sendMessage[M: Encoder](msg: M): F[Unit] =
    jsonOut.send(msg.asJson)

  private def cancelMessage(request: RequestID): UserRequest =
    UserRequest.simple(Cancel, request)

  protected def analyzeResult(
    dest: StreamEndpoint[F],
    bytes: Array[Byte],
    result: QueueOfferResult
  ): Unit =
    val suffix = s" for ${bytes.length} bytes of ${dest.describe}"
    result match
      case Enqueued    => ()
      case Dropped     => log.warn(s"Offer dropped$suffix")
      case Failure(t)  => log.error(s"Offer failed$suffix", t)
      case QueueClosed => () // log.error(s"Queue closed$suffix")

  protected def onOfferError(
    t: Throwable,
    request: RequestID,
    dest: StreamEndpoint[F],
    bytes: Array[Byte]
  ): F[Boolean] = t match
    case iae: IllegalStateException if Option(iae.getMessage).contains(DetachedMessage) =>
      log.info(s"Client disconnected '$request'.")
      remove(request, shouldAbort = true, wasSuccess = false)
    case other: Throwable =>
      log.error(s"Offer of ${bytes.length} bytes failed for '$request'.", other)
      remove(request, shouldAbort = true, wasSuccess = false)

  private def get(request: RequestID): Option[StreamEndpoint[F]] = ongoing.get(request)
