package com.mle.messaging.gcm

import com.mle.messaging.{MessagingClient, PushConstants}
import scala.concurrent.Future
import com.ning.http.client.Response
import PushConstants._

/**
 *
 * @author mle
 */
object GcmClient extends GoogleMessaging("AIzaSyCCDniLRhlHAfnXIJnsVn-You2QQKLfrM8") with MessagingClient[GcmUrl] {
  def send(dest: GcmUrl): Future[Response] = send(dest.id, Map(CMD -> "stop", TAG -> dest.tag))
}