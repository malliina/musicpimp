package com.malliina.musicpimp.cloud

import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import java.util.concurrent.{Executors, TimeUnit}

import com.malliina.concurrent.ExecutionContexts
import com.malliina.musicpimp.cloud.PimpMessages.{RangedTrack, Track}
import com.malliina.musicpimp.cloud.TrackUploads.log
import com.malliina.musicpimp.http.{MultipartRequest, TrustAllMultipartRequest}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{PimpUrl, RequestID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.{Util, Utils}
import play.api.Logger

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object TrackUploads {
  private val log = Logger(getClass)

  val uploadPath = "/track"

  def apply(host: PimpUrl) = new TrackUploads(host + uploadPath, ExecutionContexts.cached)
}

class TrackUploads(uploadUri: PimpUrl, ec: ExecutionContext) extends AutoCloseable {
  val scheduler = Executors.newSingleThreadScheduledExecutor()
  private val ongoing = TrieMap.empty[RequestID, TrustAllMultipartRequest]

  /** Uploads `track` to the cloud. Sets `request` in the `REQUEST_ID` header and uses this server's ID as the username.
    *
    * @param track   track to upload
    * @param request request id
    * @return
    */
  def upload(track: Track, request: RequestID): Future[Unit] =
  withUpload(track.id, request, file => Files.size(file).bytes, (file, req) => req.addFile(file))

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): Future[Unit] = {
    val range = rangedTrack.range
    withUpload(rangedTrack.id, request, _ => range.contentSize, (file, req) => {
      if (range.isAll) {
        log info s"Uploading $file, request $request"
        req.addFile(file)
      } else {
        log info s"Uploading $file, $range, request $request"
        req.addRangedFile(file, range)
      }
    })
  }

  def cancelSoon(request: RequestID) = cancelIn(request, 5.seconds)

  def cancelIn(request: RequestID, after: FiniteDuration) =
    scheduler.schedule(Utils.runnable(cancel(request)), after.toSeconds, TimeUnit.SECONDS)

  def cancel(request: RequestID) = ongoing.remove(request) foreach { httpRequest =>
    httpRequest.close()
    log info s"Canceled $request"
  }

  private def withUpload(trackID: TrackID,
                         request: RequestID,
                         sizeCalc: Path => StorageSize,
                         content: (Path, MultipartRequest) => Unit): Future[Unit] = {
    val trackOpt = Library.findAbsolute(trackID)
    implicit val ex = ec
    trackOpt map { path =>
      Future {
        uploadMedia(uploadUri, trackID, path, request, sizeCalc, content)
      } recover {
        case e: Exception =>
          log.warn(s"Upload of track $trackID with request ID $request terminated exceptionally.", e)
      }
    } getOrElse {
      val msg = s"Unable to find track: $trackID"
      log warn msg
      Future.failed(new FileNotFoundException(msg))
    }

  }

  /** Blocks until the upload completes.
    */
  private def uploadMedia(uploadUri: PimpUrl,
                          trackID: TrackID,
                          path: Path,
                          request: RequestID,
                          sizeCalc: Path => StorageSize,
                          content: (Path, MultipartRequest) => Unit): Unit = {
    def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request."
    Util.using(new TrustAllMultipartRequest(uploadUri.url)) { req =>
      req.addHeaders(CloudStrings.RequestId -> request.id)
      Clouds.loadID().foreach(id => req.setAuth(id.id, "pimp"))
      content(path, req)
      val response = stored(request, req, req.execute())
      val code = response.getStatusLine.getStatusCode
      val isSuccess = code >= 200 && code < 300
      if (!isSuccess) {
        log error appendMeta(s"Non-success response code $code for track ID $trackID")
      } else {
        val prefix = s"Uploaded ${sizeCalc(path)} of $trackID"
        log info appendMeta(s"$prefix with response $code")
      }
    }
  }

  private def stored[T](request: RequestID, uploadRequest: TrustAllMultipartRequest, body: => T): T = {
    ongoing.put(request, uploadRequest)
    try {
      body
    } finally {
      ongoing.remove(request)
    }
  }

  def close(): Unit = scheduler.shutdown()
}
