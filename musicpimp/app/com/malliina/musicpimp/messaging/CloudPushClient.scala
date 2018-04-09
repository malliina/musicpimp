package com.malliina.musicpimp.messaging

import com.malliina.http._
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, PushValue}
import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.PushException
import controllers.musicpimp.Rest
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CloudPushClient(host: FullUrl) {
  val pushUrl = host append "/push"

  def push(pushTask: PushTask)(implicit ec: ExecutionContext): Future[PushResult] =
    Rest.defaultClient.postJson(pushUrl, Json.obj(Cmd -> PushValue, Body -> pushTask)).flatMap { res =>
      if (res.code == 200) {
        res.parse[PushResponse].map { response =>
          Future.successful(response.result)
        }.getOrElse {
          Future.failed(new PushJsonException(res))
        }
      } else {
        Future.failed(new OkPushException(res))
      }
    }
}

object CloudPushClient {
  val default = CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""))
  val local = CloudPushClient(FullUrl("http", "localhost:9000", ""))

  def apply(host: FullUrl): CloudPushClient = new CloudPushClient(host)
}

class PushJsonException(response: HttpResponse)
  extends PushException(s"Unexpected push response body: '${response.asString}'.")

class OkPushException(val response: OkHttpResponse) extends PushException("Request failed")
