package com.mle.http

import com.ning.http.client.{AsyncCompletionHandler, Response, AsyncHttpClient}
import scala.concurrent.{ExecutionContext, Promise, Future}
import com.ning.http.util.Base64
import play.api.libs.json.{Json, JsValue}
import com.mle.http.AsyncHttp._
import java.io.Closeable

/**
 * A Scala [[Future]]s-based HTTP client. Wraps ning's async http client.
 *
 * Usage:
 * ```
 * import com.mle.http.AsyncHttp._
 * val response: Future[Response] = AsyncHttp.get("http://www.google.com")
 * ```
 *
 * @author mle
 */
object AsyncHttp {
  type RequestBuilder = AsyncHttpClient#BoundRequestBuilder
  val AUTHORIZATION = "Authorization"
  val BASIC = "Basic"
  val CONTENT_TYPE = "Content-Type"
  val JSON = "application/json"
  val WWW_FORM_URL_ENCODED = "application/x-www-form-urlencoded"

  def get(url: String)(implicit ec: ExecutionContext): Future[Response] = withClient(_.get(url))

  def postJson(url: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit ec: ExecutionContext): Future[Response] =
    execute(_.post(url, body), headers)

  def post(url: String, body: String, headers: Map[String, String] = Map.empty)(implicit ec: ExecutionContext): Future[Response] =
    execute(_.post(url, body), headers)

  def execute(f: AsyncHttp => RequestBuilder, headers: Map[String, String])(implicit ec: ExecutionContext): Future[Response] =
    withClient(c => {
      val builder = f(c)
      headers.foreach(p => builder.setHeader(p._1, p._2))
      builder
    })

  private def withClient(f: AsyncHttp => RequestBuilder)(implicit ec: ExecutionContext): Future[Response] = {
    val client = new AsyncHttp
    val response = f(client).run()
    response.onComplete(_ => client.close())
    response
  }

  implicit class RichRequestBuilder(builder: RequestBuilder) {
    def addParameters(parameters: (String, String)*) = {
      parameters.foreach(pair => builder.addParameter(pair._1, pair._2))
      builder
    }

    def basicAuthHeaderValue(username: String, password: String) = {
      val encodedCredentials = Base64.encode(s"$username:$password".getBytes)
      s"$BASIC $encodedCredentials"
    }

    def setBasicAuth(username: String, password: String): RequestBuilder = {
      builder.setHeader(AUTHORIZATION, basicAuthHeaderValue(username, password))
    }

    def run(): Future[Response] = {
      val handler = new PromisingHandler
      builder execute handler
      handler.future
    }
  }

  private class PromisingHandler extends AsyncCompletionHandler[Response] {
    private val promise = Promise[Response]()

    override def onCompleted(response: Response): Response = {
      promise success response
      response
    }

    override def onThrowable(t: Throwable): Unit = {
      promise failure t
      super.onThrowable(t)
    }

    def future = promise.future
  }

}

class AsyncHttp extends Closeable {
  val client = new AsyncHttpClient()

  def get(url: String): RequestBuilder =
    client.prepareGet(url)

  def post(url: String, body: JsValue): RequestBuilder =
    client.preparePost(url)
      .setHeader(CONTENT_TYPE, JSON)
      .setBody(Json stringify body)

  def post(url: String, body: String): RequestBuilder =
    client.preparePost(url).setBody(body)

  def close() = client.close()
}