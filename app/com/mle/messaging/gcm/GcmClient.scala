package com.mle.messaging.gcm

import com.mle.messaging.{AndroidMessagingClient, MessagingClient, PushConstants}
import scala.concurrent.Future
import com.ning.http.client.Response
import PushConstants._

/**
 *
 * @author mle
 */
object GcmClient extends GoogleMessaging("AIzaSyCCDniLRhlHAfnXIJnsVn-You2QQKLfrM8") with AndroidMessagingClient