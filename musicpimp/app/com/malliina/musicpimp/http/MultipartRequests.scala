package com.malliina.musicpimp.http

import java.io.FileInputStream
import java.nio.file.Path

import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.play.ContentRange
import controllers.musicpimp.Rest
import okhttp3.{MultipartBody, RequestBody}
import org.apache.commons.io.IOUtils
import play.api.Logger

import scala.concurrent.Future

object MultipartRequests {
  private val log = Logger(getClass)

  def rangedFile(url: FullUrl,
                 headers: Map[String, String],
                 file: Path,
                 range: ContentRange): Future[OkHttpResponse] = {
    val rangedStream = new RangedInputStream(new FileInputStream(file.toFile), range)
    val bytes = IOUtils.toByteArray(rangedStream)
    val body = RequestBody.create(null, bytes)
    withParts(url, headers, file.getFileName.toString, body)
  }

  def file(url: FullUrl,
           headers: Map[String, String],
           file: Path): Future[OkHttpResponse] = {
    val filePart = RequestBody.create(null, file.toFile)
    withParts(url, headers, file.getFileName.toString, filePart)
  }

  private def withParts(url: FullUrl, headers: Map[String, String], filename: String, part: RequestBody) = {
    val bodyBuilder = new MultipartBody.Builder()
    val body = bodyBuilder.addFormDataPart("file", filename, part).build()
    log.info(s"Uploading to '$url'...")
    val client = if (url.proto == "https") Rest.sslClient else Rest.defaultClient
    client.post(url, body, headers)
  }
}
