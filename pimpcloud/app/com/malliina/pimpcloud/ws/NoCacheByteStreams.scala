package com.malliina.pimpcloud.ws

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpResponse
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, QueueOfferResult, StreamDetachedException}
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{PimpServerSocket, UserRequest}
import com.malliina.musicpimp.json.PlaybackStrings.TrackKey
import com.malliina.musicpimp.json.SocketStrings.Cancel
import com.malliina.musicpimp.models.{CloudID, RangedRequest, RequestID, WrappedID}
import com.malliina.pimpcloud.PimpStream
import com.malliina.pimpcloud.streams.{ChannelInfo, StreamEndpoint}
import com.malliina.pimpcloud.ws.NoCacheByteStreams.{ByteStringBufferSize, DetachedMessage, log}
import com.malliina.play.http.{HttpConstants, Proxies}
import com.malliina.play.streams.StreamParsers
import com.malliina.play.{ContentRange, Streaming}
import com.malliina.ws.Streamer
import play.api.Logger
import play.api.http.{HttpEntity, HttpErrorHandler}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

object NoCacheByteStreams {
  private val log = Logger(getClass)

  // backpressures automatically, seems to work fine, and does not consume RAM
  val ByteStringBufferSize = 0
  val DetachedMessage = "Stream is terminated. SourceQueue is detached"
}

/** For each incoming request:
  *
  * 1) Assign an ID to the request
  * 2) Open a channel (or create a promise) onto which we push the eventual response
  * 3) Forward the request along with its ID to the destination server
  * 4) The destination server tags its response with the request ID
  * 5) Read the request ID from the response and push the response to the channel (or complete the promise)
  * 6) EOF and close the channel; this completes the request-response cycle
  */
class NoCacheByteStreams(
  id: CloudID,
  val jsonOut: ActorRef,
  val mat: Materializer,
  errorHandler: HttpErrorHandler,
  onUpdate: () => Unit
) extends Streamer {

  implicit val ec = mat.executionContext
  private val iteratees = TrieMap.empty[RequestID, StreamEndpoint]

  def parser(request: RequestID): Option[BodyParser[MultipartFormData[Long]]] =
    get(request) map { info =>
      // info.send is called sequentially, i.e. the next send call occurs only after the previous call has completed
      StreamParsers.multiPartByteStreaming(
        bytes =>
          info
            .send(bytes)
            .map(analyzeResult(info, bytes, _))
            .recover(onOfferError(request, info, bytes)),
        maxUploadSize,
        errorHandler
      )(mat)
    }

  def snapshot: Seq[PimpStream] = iteratees.map {
    case (uuid, stream) => PimpStream(uuid.toId, id, stream.track, stream.range)
  }.toSeq

  /**
    * @return a Result if the server received the upload request, None otherwise
    * @see https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Result = {
    val request = RequestID.random()
    val userAgent = req.headers
      .get(HeaderNames.USER_AGENT)
      .map(ua => s"user agent $ua")
      .getOrElse("unknown user agent")
    val (queue, source) = Streaming.sourceQueue[ByteString](mat, ByteStringBufferSize)
    iteratees += (request -> new ChannelInfo(queue, id, track, range))
    streamChanged()
    val address = Proxies.realAddress(req)
    log info s"Created stream '$request' of track '${track.title}' with range '${range.description}' for '$userAgent' from '$address'."
    // Watches completion and disposes of resources
    // AFAIK this is redundant, because we dispose the resources when:
    // a) the server completes its upload or b) offering data to the client fails
    val src = source.watchTermination()((_, task) =>
      task.onComplete { res =>
        remove(request, shouldAbort = true, wasSuccess = res.isSuccess)
      }
    )
    connectSource(request, src, track, range)
  }

  def exists(request: RequestID): Boolean = iteratees contains request

  override def remove(
    request: RequestID,
    shouldAbort: Boolean,
    wasSuccess: Boolean
  ): Future[Boolean] = {
    val desc = if (wasSuccess) "successful" else "failed"
    val description = s"$desc request '$request'"
    val disposal = disposeUUID(request).map { fut =>
      fut.map { _ =>
        log info s"Removed $description"
      }.recover {
        case ist: IllegalStateException if Option(ist.getMessage).contains(DetachedMessage) =>
          log info s"Removed $description after detachment"
        case sde: StreamDetachedException =>
          val msg = s"Removed $description after exceptional detachment"
          if (wasSuccess) log.debug(msg, sde) else log.warn(msg, sde)
        case e: Exception =>
          log.error(s"Removed but failed to close $description $e", e)
      }.map(_ => true)
    }.getOrElse {
      // This method is fired multiple times in normal circumstances
      log debug s"Unable to remove '$request'. Request ID not found."
      Future.successful(false)
    }
    if (shouldAbort) {
      sendMessage(cancelMessage(request))
    }
    disposal
  }

  protected def connectSource(
    request: RequestID,
    source: Source[ByteString, _],
    track: Track,
    range: ContentRange
  ): Result = {
    val status = if (range.isAll) Results.Ok else Results.PartialContent
    val entity = HttpEntity.Streamed(
      source,
      Option(range.contentLength.toLong),
      Option(HttpConstants.AudioMpeg)
    )
    val result = status.sendEntity(entity)
    connect(request, track, range)
    result
  }

  private def connect(request: RequestID, track: Track, range: ContentRange): Unit = {
    val req = buildTrackRequest(request, track, range)
    sendMessage(req)
  }

  /** Transfer complete.
    *
    * @param request the transfer ID
    */
  private def disposeUUID(request: RequestID): Option[Future[QueueOfferResult]] = {
    (iteratees remove request).map { e =>
      streamChanged()
      e.close()
    }
  }

  private def buildTrackRequest(request: RequestID, track: Track, range: ContentRange) = {
    val requestJson =
      if (range.isAll) Json.toJson(WrappedID.forId(track.id))
      else Json.toJson(RangedRequest(track.id, range))
    UserRequest(TrackKey, requestJson, request, PimpServerSocket.nobody)
  }

  // Sends `msg` to the MusicPimp server
  protected def sendMessage[M: Writes](msg: M): Unit =
    jsonOut ! Json.toJson(msg)

  protected def cancelMessage(request: RequestID) =
    UserRequest.simple(Cancel, request)

  protected def streamChanged(): Unit = onUpdate()

  protected def analyzeResult(
    dest: StreamEndpoint,
    bytes: ByteString,
    result: QueueOfferResult
  ): Unit = {
    val suffix = s" for ${bytes.length} bytes of ${dest.describe}"
    result match {
      case Enqueued    => ()
      case Dropped     => log.warn(s"Offer dropped$suffix")
      case Failure(t)  => log.error(s"Offer failed$suffix", t)
      case QueueClosed => () //log.error(s"Queue closed$suffix")
    }
  }

  protected def onOfferError(
    request: RequestID,
    dest: StreamEndpoint,
    bytes: ByteString
  ): PartialFunction[Throwable, Unit] = {
    case iae: IllegalStateException if Option(iae.getMessage).contains(DetachedMessage) =>
      log info s"Client disconnected '$request'."
      remove(request, shouldAbort = true, wasSuccess = false)
    case other: Throwable =>
      log.error(s"Offer of ${bytes.length} bytes failed for '$request'.", other)
      remove(request, shouldAbort = true, wasSuccess = false)
  }

  private def get(request: RequestID): Option[StreamEndpoint] = iteratees get request
}
