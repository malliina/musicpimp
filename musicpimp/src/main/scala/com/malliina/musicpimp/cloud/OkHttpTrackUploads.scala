package com.malliina.musicpimp.cloud

import cats.effect.Async
import cats.effect.implicits.genTemporalOps_
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.malliina.http.{FullUrl, HttpHeaders, HttpResponse}
import com.malliina.musicpimp.cloud.OkHttpTrackUploads.log
import com.malliina.musicpimp.http.{HttpConstants, MultipartRequests}
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.models.{RequestID, TrackID}
import com.malliina.play.ContentRange
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.AppLogger
import com.malliina.ws.HttpUtil

import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.Files
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object OkHttpTrackUploads:
  private val log = AppLogger(getClass)

  val uploadPath = "/track"

  def apply[F[_]: Async](lib: MusicLibrary[F], host: FullUrl) =
    new OkHttpTrackUploads(lib, host + uploadPath)

class OkHttpTrackUploads[F[_]: Async](
  lib: MusicLibrary[F],
  uploadUri: FullUrl
) extends AutoCloseable:
  val F = Async[F]
  val uploader: MultipartRequests[F] =
    ??? // = new MultipartRequests(uploadUri.url.startsWith("https"))

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
  def upload(track: TrackID, request: RequestID): F[Unit] =
    performUpload(track, request, None)

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): F[Unit] =
    val range = rangedTrack.range
    val requestRange = if range.isAll then None else Option(range)
    performUpload(rangedTrack.id, request, requestRange)

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration) =
    cancel(request).delayBy(after)

  def cancel(request: RequestID): F[Unit] =
    F.delay(uploader.cancel(request))
      .map: wasCancelled =>
        if wasCancelled then log.info(s"Cancelled $request")

  private def performUpload(
    trackID: TrackID,
    request: RequestID,
    range: Option[ContentRange]
  ): F[Unit] =
    lib
      .findFile(trackID)
      .flatMap: maybeAbsolute =>
        maybeAbsolute
          .map: file =>
            val totalSize = range.fold(Files.size(file).bytes)(_.contentSize)
            val authHeaders = Clouds
              .loadID()
              .map(id =>
                Map(HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(id.id, "pimp"))
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
            uploadRequest.map: _ =>
              log.info(s"Upload of $request complete.")
            logUpload(trackID, request, uploadRequest, totalSize)
          .getOrElse:
            val msg = s"Unable to find track: $trackID"
            log.warn(msg)
            F.raiseError(new FileNotFoundException(msg))

  /** Blocks until the upload completes.
    */
  private def logUpload(
    track: TrackID,
    request: RequestID,
    task: F[HttpResponse],
    totalSize: StorageSize
  ): F[Unit] =
    def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request"

    task
      .map: response =>
        if response.isSuccess then
          val prefix = s"Uploaded $totalSize of $track"
          log.info(appendMeta(s"$prefix with response ${response.code}."))
        else
          val len = response.body.length
          val contentType =
            response.headers
              .get(HttpHeaders.`Content-Type`)
              .flatMap(_.headOption)
              .getOrElse("unknown")
          log.error(
            appendMeta(
              s"Non-success response code ${response.code} len $len type $contentType for track $track."
            )
          )
      .handleError:
        case se: SocketException if Option(se.getMessage) contains "Socket closed" =>
          // thrown when the upload is cancelled, see method cancel
          // we cancel uploads at the request of the server if the recipient (mobile client) has disconnected
          log.info(s"Aborted upload of $request")
        case e: Exception =>
          log.warn(s"Upload of track $track with request ID $request terminated exceptionally", e)

  def close(): Unit = ()
//    scheduler.awaitTermination(3, TimeUnit.SECONDS)
//    scheduler.shutdown()
//    Try(uploader.close())
