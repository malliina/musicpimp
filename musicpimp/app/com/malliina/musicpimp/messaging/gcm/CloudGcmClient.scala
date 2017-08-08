package com.malliina.musicpimp.messaging.gcm

import com.malliina.http.WebResponse
import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.cloud.{CloudPushClient, GCMRequest, PushTask}
import com.malliina.push.MessagingClient
import com.malliina.push.gcm.GCMMessage

import scala.concurrent.{ExecutionContext, Future}

class CloudGcmClient()(implicit ec: ExecutionContext) extends MessagingClient[GCMDevice] {
  def message(tag: ServerTag) = GCMMessage(Map(Cmd -> Stop, Tag -> tag.tag))

  override def send(dest: GCMDevice): Future[WebResponse] = {
    val request = GCMRequest(Seq(dest.id), message(dest.tag))
    val task = PushTask(gcm = Option(request))
    CloudPushClient.default.push(task)
  }
}
