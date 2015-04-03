package com.mle.musicpimp.messaging.gcm

import com.mle.musicpimp.messaging.AndroidDevice
import com.mle.push.MessagingClient
import com.mle.push.gcm.GCMClient
import com.ning.http.client.Response

import scala.concurrent.Future

/**
 * What can a rogue actor do with the api key?
 *
 * @author mle
 */
object GcmClient extends GCMClient(apiKey = "AIzaSyCCDniLRhlHAfnXIJnsVn-You2QQKLfrM8") with MessagingClient[AndroidDevice] {
  override def send(dest: AndroidDevice): Future[Response] = send(dest.id, Map("cmd" -> "stop", "tag" -> dest.tag))
}