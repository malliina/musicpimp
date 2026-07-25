package com.malliina.musicpimp.cloud

import cats.effect.Async
import cats.effect.implicits.genTemporalOps_
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.ApacheTrackUploads.log
import com.malliina.musicpimp.http.{MultipartRequest, TrustAllMultipartRequest}
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.models.{RequestID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.{AppLogger, Util}

import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.{Files, Path}
import javax.net.ssl.SSLException
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ApacheTrackUploads:
  private val log = AppLogger(getClass)

  val uploadPath = "/track"

  def apply[F[_]: Async](lib: MusicLibrary[F], host: FullUrl) =
    new ApacheTrackUploads(lib, host + uploadPath)

class ApacheTrackUploads[F[_]: Async](lib: MusicLibrary[F], uploadUri: FullUrl)
  extends AutoCloseable:
  val F = Async[F]
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
  def upload(track: TrackID, request: RequestID): F[Unit] =
    withUploadApache(
      track,
      request,
      file => Files.size(file).bytes,
      (file, req) =>
        log.info(s"Uploading entire $file, request $request")
        req.addFile(file)
    )

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): F[Unit] =
    val range = rangedTrack.range
    withUploadApache(
      rangedTrack.id,
      request,
      _ => range.contentSize,
      (file, req) =>
        if range.isAll then
          log.info(s"Uploading $file, request $request")
          req.addFile(file)
        else
          log.info(s"Uploading $file, $range, request $request")
          req.addRangedFile(file, range)
    )

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration): F[Unit] =
    F.delay(cancel(request))
      .delayBy(after)

  def cancel(request: RequestID): Unit = ongoing
    .remove(request)
    .foreach: httpRequest =>
      httpRequest.request.abort()
      httpRequest.close()
      log.info(s"Cancelled '$request'.")

  private def withUploadApache(
    trackID: TrackID,
    request: RequestID,
    sizeCalc: Path => StorageSize,
    content: (Path, MultipartRequest) => Unit
  ): F[Unit] =
    lib
      .findFile(trackID)
      .flatMap: maybePath =>
        maybePath
          .map: path =>
            F.delay:
              uploadMediaApache(uploadUri, trackID, path, request, sizeCalc, content)
            .handleError:
                case se: SocketException if Option(se.getMessage) contains "Socket closed" =>
                  // thrown when the upload is cancelled, see method cancel
                  // we cancel uploads at the request of the server if the recipient (mobile client) has disconnected
                  log.info(s"Aborted upload of $request")
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
            log.warn(msg)
            F.raiseError(new FileNotFoundException(msg))

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
        log.error(
          appendMeta(
            s"Non-success response code $code len $len type $contentType for track $trackID"
          )
        )
      else
        val prefix = s"Uploaded ${sizeCalc(path)} of $trackID"
        log.info(appendMeta(s"$prefix with response $code"))

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

  def close(): Unit = ()
