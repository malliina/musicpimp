package com.malliina.musicpimp.messaging.mpns

import com.malliina.musicpimp.messaging.cloud._
import com.malliina.push.mpns.{PushUrl, ToastMessage}

class MPNSBuilder {
  def buildRequest(dest: PushUrl) = {
    val message = ToastMessage(
      "MusicPimp",
      "Tap to stop",
      s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${dest.tag}",
      silent = dest.silent)
    MPNSPayload(token = dest.url, MPNSRequest(toast = Option(message)))
  }
}
