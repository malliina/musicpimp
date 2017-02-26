package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.actor.ActorRef
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, QueueOfferResult}
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{PimpServerSocket, UserRequest}
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings.{Id, Range, TrackKey}
import com.malliina.pimpcloud.streams.{ChannelInfo, StreamEndpoint}
import com.malliina.pimpcloud.ws.NoCacheByteStreams.{Cancel, log}
import com.malliina.play.streams.StreamParsers
import com.malliina.play.{ContentRange, Streaming}
import com.malliina.ws.Streamer
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.util.Try

object NoCacheByteStreams {
  private val log = Logger(getClass)

  // backpressures automatically, seems to work fine, and does not consume RAM
  val ByteStringBufferSize = 0
  val Cancel = "cancel"
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
class NoCacheByteStreams(id: CloudID,
                         val jsonOut: ActorRef,
                         //                         val channel: SourceQueue[JsValue],
                         val mat: Materializer,
                         val onUpdate: () => Unit)
  extends Streamer {

  implicit val ec = mat.executionContext
  private val iteratees = TrieMap.empty[UUID, StreamEndpoint]

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]] = {
    get(uuid) map { info =>
      // info.send is called sequentially, i.e. the next send call occurs only after the previous call has completed
      StreamParsers.multiPartByteStreaming(
        bytes => info.send(bytes)
          .map(analyzeResult(info, bytes, _))
          .recoverWith(onOfferError(uuid, info, bytes))
          .map(_ => ()),
        maxUploadSize)(mat)
    }
  }

  def snapshot: Seq[StreamData] = iteratees.map {
    case (uuid, stream) => StreamData(uuid, id, stream.track, stream.range)
  }.toSeq

  /**
    * @return a Result if the server received the upload request, None otherwise
    * @see https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Result = {
    val uuid = UUID.randomUUID()
    val userAgent = req.headers.get(HeaderNames.USER_AGENT).map(ua => s"user agent $ua") getOrElse "unknown user agent"
    val (queue, source) = Streaming.sourceQueue[ByteString](mat, NoCacheByteStreams.ByteStringBufferSize)
    iteratees += (uuid -> new ChannelInfo(queue, id, track, range))
    streamChanged()
    log.info(s"Created stream $uuid of track ${track.title} with range ${range.description} for $userAgent from ${req.remoteAddress}")
    // Watches completion and disposes of resources early if the client (= mobile device) disconnects mid-request
    val src = source.watchTermination()((_, task) => task.onComplete(res => {
      remove(uuid, shouldAbort = true, wasSuccess = res.isSuccess)
    }))
    connectSource(uuid, src, track, range)
  }

  def exists(uuid: UUID): Boolean = iteratees contains uuid

  override def remove(uuid: UUID, shouldAbort: Boolean, wasSuccess: Boolean): Future[Boolean] = {
    val desc = if (wasSuccess) "successful" else "failed"
    val description = s"$desc request $uuid"
    val detachedMessage = "Stream is terminated. SourceQueue is detached"
    val disposal = disposeUUID(uuid) map { fut =>
      fut.map(_ => log.info(s"Removed $description")).recover {
        case ist: IllegalStateException if Option(ist.getMessage).contains(detachedMessage) =>
          log info s"Removed $description"
        case t =>
          log.error(s"Removed but failed to dispose $description", t)
      }.map(_ => true)
    } getOrElse {
      Future.successful(false)
    }
    if (shouldAbort) {
      sendMessage(cancelMessage(uuid))
    }
    disposal
  }

  protected def connectSource(uuid: UUID,
                              source: Source[ByteString, _],
                              track: Track,
                              range: ContentRange): Result = {
    val result = resultify(source, range)
    connect(uuid, track, range)
    result
    //    val connectSuccess = connect(uuid, track, range)
    //    connectSuccess.map(isSuccess => if (isSuccess) Option(result) else None)
  }

  protected def resultify(source: Source[ByteString, _], range: ContentRange): Result = {
    val status = if (range.isAll) Results.Ok else Results.PartialContent
    status.sendEntity(HttpEntity.Streamed(source, Option(range.contentLength.toLong), None))
  }

  /**
    * @return true if the server received the upload request, false otherwise
    */
  private def connect(uuid: UUID, track: Track, range: ContentRange): Unit = {
    val request = buildTrackRequest(uuid, track, range)
    sendMessage(request)
    //    def fail(): Boolean = {
    //      remove(uuid, shouldAbort = true, wasSuccess = false)
    //      false
    //    }
    //
    //    val suffix = s"$uuid for ${track.title} with range $range"
    //    sendMessage(request) map {
    //      case Enqueued =>
    //        log debug s"Connected $suffix"
    //        true
    //      case other =>
    //        log error s"Encountered $other for $suffix"
    //        fail()
    //    } recover {
    //      case t =>
    //        log.error(s"Failed to connect $suffix", t)
    //        fail()
    //    }
  }

  /** Transfer complete.
    *
    * TODO Since the transfer may have been cancelled prematurely by the recipient,
    * inform the server that it should stop uploading, to save network bandwidth.
    *
    * @param uuid the transfer ID
    */
  private def disposeUUID(uuid: UUID): Option[Future[StreamEndpoint]] = {
    (iteratees remove uuid) map { e =>
      Try(streamChanged()) recover {
        case t => log.error(s"Unable to notify of changed streams", t)
      }
      e.close().map(_ => e)
    }
  }

  private def buildTrackRequest(uuid: UUID, track: Track, range: ContentRange) = {
    val body =
      if (range.isAll) PimpServerSocket.idBody(track.id)
      else PimpServerSocket.body(Id -> track.id, Range -> range)
    UserRequest(TrackKey, body, uuid, PimpServerSocket.nobody)
  }

  // Sends `msg` to the MusicPimp server
  protected def sendMessage[M: Writes](msg: M) = jsonOut ! Json.toJson(msg)

  protected def cancelMessage(uuid: UUID) = UserRequest(Cancel, Json.obj(), uuid, PimpServerSocket.nobody)

  protected def streamChanged(): Unit = onUpdate()

  protected def analyzeResult(dest: StreamEndpoint, bytes: ByteString, result: QueueOfferResult) = {
    val suffix = s" for ${bytes.length} bytes of ${dest.describe}"
    result match {
      case Enqueued => ()
      case Dropped => log.warn(s"Offer dropped$suffix")
      case Failure(t) => log.error(s"Offer failed$suffix", t)
      case QueueClosed => () //log.error(s"Queue closed$suffix")
    }
  }

  protected def onOfferError(uuid: UUID, dest: StreamEndpoint, bytes: ByteString): PartialFunction[Throwable, Future[Boolean]] = {
    case iae: IllegalStateException if Option(iae.getMessage).contains("Stream is terminated. SourceQueue is detached") =>
      log.info(s"Client disconnected $uuid")
      remove(uuid, shouldAbort = true, wasSuccess = false)
    case other: Throwable =>
      log.error(s"Offer of ${bytes.length} bytes failed for request $uuid", other)
      remove(uuid, shouldAbort = true, wasSuccess = false)
  }

  private def get(uuid: UUID): Option[StreamEndpoint] = iteratees get uuid
}
