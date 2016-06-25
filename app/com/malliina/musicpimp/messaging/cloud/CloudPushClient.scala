package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.AsyncHttp
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, Push}
import org.asynchttpclient.Response
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future

class CloudPushClient(host: String) {
  val pushUrl = s"$host/push"

  def push(pushTask: PushTask): Future[Response] =
    AsyncHttp.postJson(pushUrl, Json.obj(Cmd -> Push, Body -> pushTask))
}

object CloudPushClient {
  val default = CloudPushClient("https://cloud.musicpimp.org")
  val local = CloudPushClient("http://localhost:9000")

  def apply(host: String) = new CloudPushClient(host)
}
