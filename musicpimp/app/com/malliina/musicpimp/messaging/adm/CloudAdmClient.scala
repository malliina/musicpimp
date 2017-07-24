package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.cloud.{ADMRequest, CloudPushClient, PushTask}
import com.malliina.push.MessagingClient
import com.malliina.push.android.AndroidMessage
import org.asynchttpclient.Response

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class CloudAdmClient()(implicit ec: ExecutionContext) extends MessagingClient[ADMDevice] {
  def message(tag: ServerTag) = AndroidMessage(Map(Cmd -> Stop, Tag -> tag.tag), 60.seconds)

  override def send(dest: ADMDevice): Future[Response] = {
    val request = ADMRequest(Seq(dest.id), message(dest.tag))
    val task = PushTask(adm = Option(request))
    CloudPushClient.default.push(task)
  }
}
