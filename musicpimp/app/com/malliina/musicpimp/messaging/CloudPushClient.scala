package com.malliina.musicpimp.messaging

import com.malliina.http.{AsyncHttp, FullUrl, WebResponse}
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, Push}
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.PushException
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask)(implicit ec: ExecutionContext): Future[PushResult] =
    AsyncHttp.postJson(pushUrl.url, Json.obj(Cmd -> Push, Body -> pushTask)).flatMap { r =>
      r.parse[PushResponse].map(response => Future.successful(response.result)).getOrElse(Future.failed(new PushJsonException(r)))
    }
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl): CloudPushClient = new CloudPushClient(host)
}

class PushJsonException(response: WebResponse) extends PushException("Unexpected push response.")
