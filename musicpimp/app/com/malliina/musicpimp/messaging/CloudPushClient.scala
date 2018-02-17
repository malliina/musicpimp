package com.malliina.musicpimp.messaging

import com.malliina.http._
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, PushValue}
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.{PushException, ResponseException}
import okhttp3.Response
import play.api.libs.json.{JsError, JsValue, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask)(implicit ec: ExecutionContext): Future[PushResult] =
    AsyncHttp.postJson(pushUrl.url, Json.obj(Cmd -> PushValue, Body -> pushTask)).flatMap { r =>
      if (r.code == 200) {
        r.parse[PushResponse].map { response =>
          Future.successful(response.result)
        }.getOrElse {
          Future.failed(new PushJsonException(r))
        }
      } else {
        Future.failed(new ResponseException(r))
      }
    }

  def okPush(pushTask: PushTask)(implicit ec: ExecutionContext): Future[PushResult] =
    OkClient.default.postJson(pushUrl, Json.obj(Cmd -> PushValue, Body -> pushTask)).flatMap { res =>
      val r = new OkResponse(res)
      if (r.code == 200) {
        r.parse[PushResponse].map { response =>
          Future.successful(response.result)
        }.getOrElse {
          Future.failed(new PushJsonException(r))
        }
      } else {
        Future.failed(new OkPushException(r))
      }
    }
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl): CloudPushClient = new CloudPushClient(host)
}

class PushJsonException(response: ResponseLike)
  extends PushException(s"Unexpected push response body: '${response.asString}'.")

class OkResponse(val inner: Response) extends ResponseLike {
  override lazy val asString = inner.body().string()

  override def json: Try[JsValue] = Try(Json.parse(asString))

  override def parse[T: Reads] = json.map(_.validate[T]).getOrElse(JsError(s"Not JSON: '$asString'."))

  override def code: Int = inner.code()
}

class OkPushException(val response: OkResponse) extends PushException("Request failed")
