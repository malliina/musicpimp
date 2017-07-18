package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.cloud.{APNSRequest, CloudPushClient, PushTask}
import com.malliina.push.MessagingClient
import com.malliina.push.apns.{APNSMessage, APSPayload}
import org.asynchttpclient.Response
import play.api.libs.json.Json.toJson

import scala.concurrent.Future

/** Sends a push notification using pimpcloud.
  */
object CloudAPNSClient extends MessagingClient[APNSDevice] {
  val Message = "Open to stop"

  override def send(dest: APNSDevice): Future[Response] = {
    val payload = APSPayload(Some(Left(Message)))
    val extra = Map(Cmd -> toJson(Stop), Tag -> toJson(dest.tag))
    val message = APNSMessage(payload, extra)
    val request = APNSRequest(Seq(dest.id), message)
    val task = PushTask(Option(request), None, None, None, None)
    CloudPushClient.default.push(task)
  }
}
