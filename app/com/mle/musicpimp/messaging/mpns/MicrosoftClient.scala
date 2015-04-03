package com.mle.musicpimp.messaging.mpns

import com.mle.push.MessagingClient
import com.mle.push.mpns.{MPNSClient, PushUrl, ToastMessage}
import com.ning.http.client.Response

import scala.concurrent.Future

/**
 *
 * @author mle
 */
object MicrosoftClient extends MPNSClient with MessagingClient[PushUrl] {
  def send(dest: PushUrl): Future[Response] =
    push(dest.url, ToastMessage("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}", dest.silent))
}
