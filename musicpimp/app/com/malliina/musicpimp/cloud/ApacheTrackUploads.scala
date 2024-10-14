package com.malliina.musicpimp.cloud

import cats.effect.IO
import com.malliina.audio.ExecutionContexts
import com.malliina.concurrent.Execution.runtime

import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.{Files, Path}
import java.util.concurrent.{Executors, TimeUnit}
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.ApacheTrackUploads.log
import com.malliina.musicpimp.http.{MultipartRequest, TrustAllMultipartRequest}
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.models.{RequestID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.Util

import javax.net.ssl.SSLException
import play.api.Logger

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object ApacheTrackUploads:
  private val log = Logger(getClass)

  val uploadPath = "/track"

  def apply(lib: MusicLibrary[IO], host: FullUrl) =
    new ApacheTrackUploads(lib, host + uploadPath, ExecutionContexts.cached)

class ApacheTrackUploads(lib: MusicLibrary[IO], uploadUri: FullUrl, ec: ExecutionContext)
  extends AutoCloseable:
  implicit val exec: ExecutionContext = ec
  val scheduler = Executors.newSingleThreadScheduledExecutor()
  private val ongoing = TrieMap.empty[RequestID, TrustAllMultipartRequest]

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
    withUploadApache(
      track,
      request,
      file => Files.size(file).bytes,
      (file, req) =>
        log info s"Uploading entire $file, request $request"
        req.addFile(file)
    )

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): Future[Unit] =
    val range = rangedTrack.range
    withUploadApache(
      rangedTrack.id,
      request,
      _ => range.contentSize,
      (file, req) =>
        if range.isAll then
          log info s"Uploading $file, request $request"
          req.addFile(file)
        else
          log info s"Uploading $file, $range, request $request"
          req.addRangedFile(file, range)
    )

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration) =
    val runnable = new Runnable:
      override def run(): Unit = cancel(request)
    scheduler.schedule(runnable, after.toSeconds, TimeUnit.SECONDS)

  def cancel(request: RequestID): Unit = ongoing.remove(request) foreach { httpRequest =>
    httpRequest.request.abort()
    httpRequest.close()
    log.info(s"Cancelled '$request'.")
  }

  private def withUploadApache(
    trackID: TrackID,
    request: RequestID,
    sizeCalc: Path => StorageSize,
    content: (Path, MultipartRequest) => Unit
  ): Future[Unit] =
    lib
      .findFile(trackID)
      .unsafeToFuture()
      .flatMap: maybePath =>
        maybePath
          .map: path =>
            Future:
              uploadMediaApache(uploadUri, trackID, path, request, sizeCalc, content)
            .recover:
              case se: SocketException if Option(se.getMessage) contains "Socket closed" =>
                // thrown when the upload is cancelled, see method cancel
                // we cancel uploads at the request of the server if the recipient (mobile client) has disconnected
                log info s"Aborted upload of $request"
              case ssl: SSLException
                  if Option(ssl.getMessage) contains "Connection or outbound has been closed" =>
                log.info(s"Cancelled upload of '$trackID' with request '$request'.")
              case e: Exception =>
                log.warn(
                  s"Upload of track $trackID with request ID $request terminated exceptionally",
                  e
                )
          .getOrElse:
            val msg = s"Unable to find track: $trackID"
            log warn msg
            Future.failed(new FileNotFoundException(msg))

  /** Blocks until the upload completes.
    */
  private def uploadMediaApache(
    uploadUri: FullUrl,
    trackID: TrackID,
    path: Path,
    request: RequestID,
    sizeCalc: Path => StorageSize,
    content: (Path, MultipartRequest) => Unit
  ): Unit =
    def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request"

    Util.using(new TrustAllMultipartRequest(uploadUri.url)): req =>
      req.addHeaders(CloudResponse.RequestKey -> request.id)
      Clouds.loadID().foreach(id => req.setAuth(id.id, "pimp"))
      content(path, req)
      val response = stored(request, req, req.execute())
      val code = response.getStatusLine.getStatusCode
      val isSuccess = code >= 200 && code < 300
      if !isSuccess then
        val entity = response.getEntity
        val len = entity.getContentLength
        val contentType = entity.getContentType.getValue
        log error appendMeta(
          s"Non-success response code $code len $len type $contentType for track $trackID"
        )
      else
        val prefix = s"Uploaded ${sizeCalc(path)} of $trackID"
        log info appendMeta(s"$prefix with response $code")

  private def stored[T](
    request: RequestID,
    uploadRequest: TrustAllMultipartRequest,
    body: => T
  ): T =
    ongoing.put(request, uploadRequest)
    try
      body
    finally
      ongoing.remove(request)

  def close(): Unit =
    scheduler.awaitTermination(3, TimeUnit.SECONDS)
    scheduler.shutdown()
