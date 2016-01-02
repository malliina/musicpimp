package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.cloud.{APNSRequest, CloudPushClient, PushTask}
import com.malliina.push.MessagingClient
import com.malliina.push.apns.{APNSMessage, APNSToken}
import com.ning.http.client.Response

import scala.concurrent.Future

/** Sends a push notification using pimpcloud.
  *
  * @author mle
  */
object PimpAPNSClient extends MessagingClient[APNSToken] {
  override def send(dest: APNSToken): Future[Response] = {
    val message = APNSMessage.simple("Open to stop")
    val request = APNSRequest(Seq(dest), message)
    val task = PushTask(Option(request), None, None, None)
    CloudPushClient.default.push(task)
  }
}
