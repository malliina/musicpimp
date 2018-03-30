package com.malliina.musicpimp.http

import java.io.FileInputStream
import java.nio.file.Path

import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.play.ContentRange
import controllers.musicpimp.Rest
import okhttp3.{MultipartBody, RequestBody}
import org.apache.commons.io.IOUtils

import scala.concurrent.Future

object MultipartRequests {
  def rangedFile(url: FullUrl,
                 headers: Map[String, String],
                 file: Path,
                 range: ContentRange): Future[OkHttpResponse] = {
    withParts(url, headers) {
      val rangedStream = new RangedInputStream(new FileInputStream(file.toFile), range)
      val bytes = IOUtils.toByteArray(rangedStream)
      RequestBody.create(Rest.audioMpeg, bytes)
    }
  }

  def file(url: FullUrl,
           headers: Map[String, String],
           file: Path): Future[OkHttpResponse] = {
    withParts(url, headers) {
      RequestBody.create(Rest.audioMpeg, file.toFile)
    }
  }

  private def withParts(url: FullUrl, headers: Map[String, String])(part: RequestBody) = {
    val client = Rest.sslClient
    val bodyBuilder = new MultipartBody.Builder()
    bodyBuilder.addPart(part)
    client.post(url, bodyBuilder.build(), headers)
  }
}
