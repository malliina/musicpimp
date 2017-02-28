package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.AsyncHttp
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, Push}
import com.malliina.play.http.FullUrl
import org.asynchttpclient.Response
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask): Future[Response] =
    AsyncHttp.postJson(pushUrl.url, Json.obj(Cmd -> Push, Body -> pushTask))
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl) = new CloudPushClient(host)
}
