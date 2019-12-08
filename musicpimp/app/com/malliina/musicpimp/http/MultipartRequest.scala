package com.malliina.musicpimp.http

import java.io.{FileInputStream, InputStream}
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

import com.malliina.play.ContentRange
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.content.{FileBody, InputStreamBody}
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntityBuilder}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import play.api.http.ContentTypes._
import play.api.http.HeaderNames._
import concurrent.duration.DurationInt
import scala.util.Try

/** The Play WS API does not afaik support multipart/form-data file uploads, therefore this class provides it using
  * Apache HttpClient.
  */
class MultipartRequest(
  uri: String,
  buildInstructions: HttpClientBuilder => HttpClientBuilder = b => b
) extends AutoCloseable {

  private val timeout = 60.minutes
  private val config = RequestConfig
    .custom()
    .setConnectTimeout(timeout.toMillis.toInt)
    .setConnectionRequestTimeout(timeout.toMillis.toInt)
    .setSocketTimeout(timeout.toMillis.toInt)
    .build()
  private val client =
    buildInstructions(HttpClientBuilder.create().setDefaultRequestConfig(config)).build()
  val request = new HttpPost(uri)
  request.addHeader(ACCEPT, JSON)
  private val reqContent =
    MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
  private val isCancelled = new AtomicBoolean(false)

  @volatile private var streams: List[InputStream] = Nil

  def setAuth(username: String, password: String): Unit = {
    val creds = new UsernamePasswordCredentials(username, password)
    request addHeader new BasicScheme().authenticate(creds, request, new BasicHttpContext())
  }

  def addHeaders(kvs: (String, String)*): Unit = kvs foreach (kv => request.addHeader(kv._1, kv._2))

  def addFile(file: Path): Unit = {
    def fileName = file.getFileName.toString
    reqContent.addPart(fileName, new FileBody(file.toFile))
  }

  def addRangedFile(file: Path, range: ContentRange): Unit = {
    if (range.isAll) {
      addFile(file)
    } else {
      val fileName = file.getFileName.toString
      val rangedStream = new RangedInputStream(new FileInputStream(file.toFile), range)
      streams = rangedStream :: streams
      val contentBody = new InputStreamBody(rangedStream, fileName)
      reqContent.addPart(fileName, contentBody)
    }
  }

  def addKeyValues(kvs: (String, String)*): Unit =
    kvs.foreach { kv =>
      val (key, value) = kv
      reqContent.addTextBody(key, value)
    }

  /** Executes the request.
    *
    * @return the response
    */
  def execute() = {
    request setEntity reqContent.build()
    client execute request
  }

  /** Executes the request.
    *
    * @return the stringified response, if any
    */
  def executeToString() = Option(execute().getEntity) map EntityUtils.toString

  override def close(): Unit = {
    val isFirstClose = isCancelled.compareAndSet(false, true)
    if (isFirstClose) {
      Try(request.abort())
      Try(client.close())
      streams.foreach(stream => Try(stream.close()))
      streams = Nil
    }
  }
}

class TrustAllMultipartRequest(uri: String)
  extends MultipartRequest(
    uri,
    _.setSSLSocketFactory(ApacheHttpHelper.allowAllCertificatesSocketFactory())
  )
