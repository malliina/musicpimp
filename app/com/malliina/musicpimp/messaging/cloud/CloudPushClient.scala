package com.malliina.musicpimp.messaging.cloud

import com.malliina.http.AsyncHttp
import com.ning.http.client.Response
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * @author mle
  */
class CloudPushClient(host: String) {
  val pushUrl = s"$host/push"

  def push(pushTask: PushTask): Future[Response] = AsyncHttp.postJson(pushUrl, Json.toJson(pushTask))
}

object CloudPushClient {
  val default = CloudPushClient("https://cloud.musicpimp.org")
  val local = CloudPushClient("http://localhost:9000")

  def apply(host: String) = new CloudPushClient(host)
}
