package com.malliina.musicpimp.cloud

import cats.effect.IO

import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.Files
import java.util.concurrent.{Executors, TimeUnit}
import com.malliina.concurrent.Execution
import com.malliina.concurrent.Execution.runtime
import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.musicpimp.cloud.OkHttpTrackUploads.log
import com.malliina.musicpimp.http.MultipartRequests
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.models.{RequestID, TrackID}
import com.malliina.play.ContentRange
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.ws.HttpUtil
import play.api.Logger
import play.api.http.HeaderNames

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object OkHttpTrackUploads:
  private val log = Logger(getClass)

  val uploadPath = "/track"

  def apply(lib: MusicLibrary[IO], host: FullUrl) =
    new OkHttpTrackUploads(lib, host + uploadPath, Execution.cached)

class OkHttpTrackUploads(lib: MusicLibrary[IO], uploadUri: FullUrl, ec: ExecutionContext)
  extends AutoCloseable:
  implicit val exec: ExecutionContext = ec
  val scheduler = Executors.newSingleThreadScheduledExecutor()
  val uploader = new MultipartRequests(uploadUri.url.startsWith("https"))

  /** Uploads `track` to the cloud. Sets `request` in the `REQUEST_ID` header and uses this server's
    * ID as the username.
    *
    * @param track
    *   track to upload
    * @param request
    *   request id
    * @return
    *   a Future that completes when the upload completes
    */
  def upload(track: TrackID, request: RequestID): Future[Unit] =
    performUpload(track, request, None)

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): Future[Unit] =
    val range = rangedTrack.range
    val requestRange = if range.isAll then None else Option(range)
    performUpload(rangedTrack.id, request, requestRange)

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration) =
    val runnable = new Runnable:
      override def run(): Unit = cancel(request)
    scheduler.schedule(runnable, after.toSeconds, TimeUnit.SECONDS)

  def cancel(request: RequestID): Unit =
    val wasCancelled = uploader.cancel(request)
    if wasCancelled then log.info(s"Cancelled $request")

  private def performUpload(
    trackID: TrackID,
    request: RequestID,
    range: Option[ContentRange]
  ): Future[Unit] =
    lib
      .findFile(trackID)
      .unsafeToFuture()
      .flatMap: maybeAbsolute =>
        maybeAbsolute
          .map: file =>
            val totalSize = range.fold(Files.size(file).bytes)(_.contentSize)
            val authHeaders = Clouds
              .loadID()
              .map(id =>
                Map(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue(id.id, "pimp"))
              )
              .getOrElse(Map.empty)
            val headers = authHeaders ++ Map(CloudResponse.RequestKey -> request.id)
            val uploadRequest = range
              .map: r =>
                log.info(s"Uploading $file, $r, request $request to $uploadUri")
                uploader.rangedFile(uploadUri, headers, file, r, request)
              .getOrElse:
                log.info(s"Uploading entire $file, request $request to $uploadUri")
                uploader.file(uploadUri, headers, file, request)
            uploadRequest.onComplete: _ =>
              log info s"Upload of $request complete."
            logUpload(trackID, request, uploadRequest, totalSize)
          .getOrElse:
            val msg = s"Unable to find track: $trackID"
            log warn msg
            Future.failed(new FileNotFoundException(msg))

  /** Blocks until the upload completes.
    */
  private def logUpload(
    track: TrackID,
    request: RequestID,
    task: Future[OkHttpResponse],
    totalSize: StorageSize
  ): Future[Unit] =
    def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request"

    task
      .map: response =>
        if response.isSuccess then
          val prefix = s"Uploaded $totalSize of $track"
          log.info(appendMeta(s"$prefix with response ${response.code}."))
        else
          val len = response.inner.body().contentLength()
          val contentType =
            Option(response.inner.body().contentType()).map(_.toString).getOrElse("unknown")
          log.error(
            appendMeta(
              s"Non-success response code ${response.code} len $len type $contentType for track $track."
            )
          )
      .recover:
        case se: SocketException if Option(se.getMessage) contains "Socket closed" =>
          // thrown when the upload is cancelled, see method cancel
          // we cancel uploads at the request of the server if the recipient (mobile client) has disconnected
          log.info(s"Aborted upload of $request")
        case e: Exception =>
          log.warn(s"Upload of track $track with request ID $request terminated exceptionally", e)

  def close(): Unit =
    scheduler.awaitTermination(3, TimeUnit.SECONDS)
    scheduler.shutdown()
    //    Try(uploader.close())
