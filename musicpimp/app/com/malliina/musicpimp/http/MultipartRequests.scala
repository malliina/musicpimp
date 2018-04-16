package com.malliina.musicpimp.http

import java.io.FileInputStream
import java.nio.file.Path

import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.musicpimp.models.RequestID
import com.malliina.play.ContentRange
import controllers.musicpimp.Rest
import okhttp3.{MultipartBody, Request, RequestBody}
import org.apache.commons.io.IOUtils
import play.api.Logger

import scala.concurrent.Future

class MultipartRequests(isHttps: Boolean) extends AutoCloseable {
  private val log = Logger(getClass)
  val client = if (isHttps) Rest.sslClient else Rest.defaultClient

  def rangedFile(url: FullUrl,
                 headers: Map[String, String],
                 file: Path,
                 range: ContentRange,
                 tag: RequestID): Future[OkHttpResponse] =
    try {
      val inStream = new FileInputStream(file.toFile)
      val bytes = try {
        val rangedStream = new RangedInputStream(inStream, range)
        IOUtils.toByteArray(rangedStream)
      } finally {
        inStream.close()
      }
      val body = RequestBody.create(null, bytes)
      withParts(url, headers, file.getFileName.toString, body, tag)
    } catch {
      case e: Exception => Future.failed(e)
    }

  def file(url: FullUrl,
           headers: Map[String, String],
           file: Path,
           tag: RequestID): Future[OkHttpResponse] = {
    val filePart = RequestBody.create(null, file.toFile)
    withParts(url, headers, file.getFileName.toString, filePart, tag)
  }

  /**
    * @param tag request tag
    * @return true if anything was cancelled, false otherwise
    */
  def cancel(tag: RequestID): Boolean = {
    val dispatcher = client.client.dispatcher()
    import collection.JavaConverters.asScalaBufferConverter
    val cancellable = (dispatcher.queuedCalls().asScala ++ dispatcher.runningCalls().asScala)
      .filter(_.request().tag() == tag)
    cancellable.foreach(_.cancel())
    cancellable.nonEmpty
  }

  private def withParts(url: FullUrl,
                        headers: Map[String, String],
                        filename: String,
                        part: RequestBody,
                        tag: RequestID): Future[OkHttpResponse] = {
    val bodyBuilder = new MultipartBody.Builder()
    val body = bodyBuilder.addFormDataPart("file", filename, part).build()
    log.info(s"Uploading to '$url'...")
    client.execute(requestFor(url, headers).post(body).tag(tag).build())
  }

  private def requestFor(url: FullUrl, headers: Map[String, String]) =
    headers.foldLeft(new Request.Builder().url(url.url)) {
      case (r, (key, value)) => r.addHeader(key, value)
    }

  def close(): Unit = client.close()
}
