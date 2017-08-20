package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.{AsyncHttp, WebResponse}
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, Push}
import com.malliina.http.FullUrl
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask)(implicit ec: ExecutionContext): Future[WebResponse] =
    AsyncHttp.postJson(pushUrl.url, Json.obj(Cmd -> Push, Body -> pushTask))
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl) = new CloudPushClient(host)
}
