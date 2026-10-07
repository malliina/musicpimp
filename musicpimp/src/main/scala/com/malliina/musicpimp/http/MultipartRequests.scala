package com.malliina.musicpimp.http

import cats.effect.Async
import com.malliina.http.io.HttpClientF2
import com.malliina.http.{FullUrl, HttpResponse}
import com.malliina.musicpimp.http.MultipartRequests.log
import com.malliina.musicpimp.models.RequestID
import com.malliina.play.ContentRange
import com.malliina.util.AppLogger
import okhttp3.{MultipartBody, Request, RequestBody}
import org.apache.commons.io.IOUtils

import java.nio.file.Path
import scala.jdk.CollectionConverters.ListHasAsScala

object MultipartRequests:
  private val log = AppLogger(getClass)

class MultipartRequests[F[_]: Async](client: HttpClientF2[F]):
  val F = Async[F]

  def rangedFile(
    url: FullUrl,
    headers: Map[String, String],
    file: Path,
    range: ContentRange,
    tag: RequestID
  ): F[HttpResponse] =
    try
      val rangedStream = RangedInputStream(file, range)
      val bytes =
        try IOUtils.toByteArray(rangedStream)
        finally rangedStream.close()
      val body = RequestBody.create(bytes, null)
      withParts(url, headers, file.getFileName.toString, body, tag)
    catch case e: Exception => F.raiseError(e)

  def file(
    url: FullUrl,
    headers: Map[String, String],
    file: Path,
    tag: RequestID
  ): F[HttpResponse] =
    val filePart = RequestBody.create(file.toFile, null)
    withParts(url, headers, file.getFileName.toString, filePart, tag)

  /** @param tag
    *   request tag
    * @return
    *   true if anything was canceled, false otherwise
    */
  def cancel(tag: RequestID): Boolean =
    val dispatcher = client.client.dispatcher()
    val cancellable = (dispatcher.queuedCalls().asScala ++ dispatcher.runningCalls().asScala)
      .filter(_.request().tag() == tag)
    cancellable.foreach(_.cancel())
    cancellable.nonEmpty

  private def withParts(
    url: FullUrl,
    headers: Map[String, String],
    filename: String,
    part: RequestBody,
    tag: RequestID
  ): F[HttpResponse] =
    val bodyBuilder = new MultipartBody.Builder()
    val body = bodyBuilder.addFormDataPart("file", filename, part).build()
    log.info(s"Uploading to '$url'...")
    client.execute(requestFor(url, headers).post(body).tag(tag).build())

  private def requestFor(url: FullUrl, headers: Map[String, String]) =
    headers.foldLeft(new Request.Builder().url(url.url)):
      case (r, (key, value)) => r.addHeader(key, value)

  def close(): Unit = ()
