package com.malliina.musicpimp.cloud

import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.Files
import java.util.concurrent.{Executors, TimeUnit}

import com.malliina.concurrent.ExecutionContexts
import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.musicpimp.cloud.TrackUploads.log
import com.malliina.musicpimp.http.MultipartRequests
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{RequestID, TrackID}
import com.malliina.play.ContentRange
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.Utils
import com.malliina.ws.HttpUtil
import play.api.Logger
import play.api.http.HeaderNames

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object TrackUploads {
  private val log = Logger(getClass)

  val uploadPath = "/track"

  def apply(host: FullUrl) = new TrackUploads(host + uploadPath, ExecutionContexts.cached)
}

class TrackUploads(uploadUri: FullUrl, ec: ExecutionContext) extends AutoCloseable {
  implicit val exec = ec
  val scheduler = Executors.newSingleThreadScheduledExecutor()
  private val ongoing = TrieMap.empty[RequestID, TrackID]

  /** Uploads `track` to the cloud. Sets `request` in the `REQUEST_ID` header and uses this server's ID as the username.
    *
    * @param track   track to upload
    * @param request request id
    * @return a Future that completes when the upload completes
    */
  def upload(track: TrackID, request: RequestID): Future[Unit] = {
    withUpload(track, request, None)
  }

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): Future[Unit] = {
    val range = rangedTrack.range
    val requestRange = if (range.isAll) None else Option(range)
    withUpload(rangedTrack.id, request, requestRange)
  }

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration) =
    scheduler.schedule(Utils.runnable(cancel(request)), after.toSeconds, TimeUnit.SECONDS)

  def cancel(request: RequestID) = ongoing.remove(request) foreach { httpRequest =>
    //    httpRequest.request.abort()
    //    httpRequest.close()
    log info s"Cancelled $request"
  }

  private def withUpload(trackID: TrackID,
                         request: RequestID,
                         range: Option[ContentRange]): Future[Unit] = {
    val trackOpt = Library.findAbsolute(trackID)
    trackOpt map { file =>
      val totalSize = range.fold(Files.size(file).bytes)(_.contentSize)
      val authHeaders = Clouds.loadID()
        .map(id => Map(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue(id.id, "pimp")))
        .getOrElse(Map.empty)
      val headers = authHeaders ++ Map(CloudResponse.RequestKey -> request.id)
      ongoing.put(request, trackID)
      val uploadRequest = range.map { r =>
        log info s"Uploading $file, $r, request $request to $uploadUri"
        MultipartRequests.rangedFile(uploadUri, headers, file, r)
      }.getOrElse {
        log info s"Uploading entire $file, request $request to $uploadUri"
        MultipartRequests.file(uploadUri, headers, file)
      }
      uploadRequest.onComplete { _ =>
        log info s"Upload of $request complete."
        ongoing.remove(request)
      }
      logUpload(trackID, request, uploadRequest, totalSize)
    } getOrElse {
      val msg = s"Unable to find track: $trackID"
      log warn msg
      Future.failed(new FileNotFoundException(msg))
    }
  }

  /** Blocks until the upload completes.
    */
  private def logUpload(track: TrackID, request: RequestID, task: Future[OkHttpResponse], totalSize: StorageSize): Future[Unit] = {
    def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request"

    task.map { response =>
      if (response.isSuccess) {
        val prefix = s"Uploaded $totalSize of $track"
        log info appendMeta(s"$prefix with response ${response.code}.")
      } else {
        val len = response.inner.body().contentLength()
        val contentType = Option(response.inner.body().contentType()).map(_.toString).getOrElse("unknown")
        log error appendMeta(s"Non-success response code ${response.code} len $len type $contentType for track $track.")
      }
    }.recover {
      case se: SocketException if Option(se.getMessage) contains "Socket closed" =>
        // thrown when the upload is cancelled, see method cancel
        // we cancel uploads at the request of the server if the recipient (mobile client) has disconnected
        log info s"Aborted upload of $request"
      case e: Exception =>
        log.warn(s"Upload of track $track with request ID $request terminated exceptionally", e)
    }
  }

  def close(): Unit = {
    scheduler.awaitTermination(3, TimeUnit.SECONDS)
    scheduler.shutdown()
  }
}
