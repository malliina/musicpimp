package com.mle.messaging.mpns

import com.mle.messaging.MessagingClient
import com.ning.http.client.Response

import scala.concurrent.Future

/**
 *
 * @author mle
 */
object MPNSClient extends MPNS with MessagingClient[PushUrl] {
  def send(dest: PushUrl): Future[Response] =
    send(dest.url, ToastMessage("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}", dest.silent))
}