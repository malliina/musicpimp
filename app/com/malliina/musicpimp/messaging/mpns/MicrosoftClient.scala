package com.malliina.musicpimp.messaging.mpns

import com.malliina.push.MessagingClient
import com.malliina.push.mpns.{MPNSClient, PushUrl, ToastMessage}
import org.asynchttpclient.Response

import scala.concurrent.Future

object MicrosoftClient extends MPNSClient with MessagingClient[PushUrl] {
  def send(dest: PushUrl): Future[Response] =
    push(dest.url, ToastMessage("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}", dest.silent))
}
