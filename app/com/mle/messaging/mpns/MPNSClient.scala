package com.mle.messaging.mpns

import scala.concurrent.Future
import com.ning.http.client.Response
import com.mle.messaging.MessagingClient

/**
 *
 * @author mle
 */
object MPNSClient extends MPNS with MessagingClient[PushUrl] {
  def send(dest: PushUrl): Future[Response] =
    send(dest.url, ToastMessage("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}", dest.silent))
}