package com.malliina.musicpimp.messaging.mpns

import com.malliina.musicpimp.messaging.cloud.{CloudPushClient, MPNSRequest, PushTask}
import com.malliina.push.MessagingClient
import com.malliina.push.mpns.{PushUrl, ToastMessage}
import org.asynchttpclient.Response

import scala.concurrent.{ExecutionContext, Future}

class CloudMicrosoftClient()(implicit ec: ExecutionContext) extends MessagingClient[PushUrl] {
  override def send(dest: PushUrl): Future[Response] = {
    val message = ToastMessage(
      "MusicPimp",
      "Tap to stop",
      s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}",
      silent = dest.silent)
    val task = PushTask(mpns = Option(MPNSRequest(tokens = Seq(dest.url), toast = Option(message))))
    CloudPushClient.default.push(task)
  }
}
