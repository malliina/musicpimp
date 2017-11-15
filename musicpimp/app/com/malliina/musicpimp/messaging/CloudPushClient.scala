package com.malliina.musicpimp.messaging

import com.malliina.http.{AsyncHttp, FullUrl, WebResponse}
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, PushValue}
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.{PushException, ResponseException}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask)(implicit ec: ExecutionContext): Future[PushResult] =
    AsyncHttp.postJson(pushUrl.url, Json.obj(Cmd -> PushValue, Body -> pushTask)).flatMap { r =>
      if (r.code == 200) {
        r.parse[PushResponse].map(response => Future.successful(response.result)).getOrElse(Future.failed(new PushJsonException(r)))
      } else {
        Future.failed(new ResponseException(r))
      }
    }
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl): CloudPushClient = new CloudPushClient(host)
}

class PushJsonException(response: WebResponse)
  extends PushException(s"Unexpected push response body: '${response.asString}'.")
