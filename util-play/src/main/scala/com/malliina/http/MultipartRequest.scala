package com.malliina.http

import java.nio.file.Path
import java.util.Base64
import com.malliina.security.SSLUtils
import okhttp3.{MediaType, MultipartBody, Request, RequestBody}
import play.api.http.ContentTypes.*
import play.api.http.HeaderNames.*

import scala.concurrent.ExecutionContext

/** The Play WS API does not afaik support multipart/form-data file uploads, therefore this class provides it using
  * Apache HttpClient.
  */
object MultipartRequest {
  def trustAll(url: FullUrl) = {
    val ssf = SSLUtils.trustAllSslContext().getSocketFactory
    val tm = SSLUtils.trustAllTrustManager()
    val client = OkClient.ssl(ssf, tm)
    new MultipartRequest(url, client)
  }
}

class MultipartRequest(url: FullUrl, val client: OkClient) extends AutoCloseable {
  implicit val ec: ExecutionContext = client.exec
  val requestBuilder = new Request.Builder().url(url.url)
  requestBuilder.addHeader(ACCEPT, JSON)
  val bodyBuilder = new MultipartBody.Builder()
  //  private val reqContent = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)

  def setAuth(username: String, password: String): Unit = {
    def authorizationValue(username: String, password: String) =
      s"Basic " + Base64.getEncoder.encodeToString((username + ":" + password).getBytes("UTF-8"))

    requestBuilder.addHeader(AUTHORIZATION, authorizationValue(username, password))
  }

  def addFile(mediaType: MediaType, file: Path): Unit =
    bodyBuilder.addFormDataPart(
      "file",
      file.getFileName.toString,
      RequestBody.create(file.toFile, mediaType)
    )

  def addKeyValues(kvs: (String, String)*): Unit =
    kvs.foreach { kv =>
      val (key, value) = kv
      bodyBuilder.addFormDataPart(key, value)
    }

  /** Executes the request.
    *
    * @return the response
    */
  def execute() = {
    val req = requestBuilder.post(bodyBuilder.build()).build()
    client.execute(req)
  }

  /** Executes the request.
    *
    * @return the stringified response, if any
    */
  def executeToString() = execute().map(_.asString)

  override def close(): Unit = {
    client.close()
  }
}
