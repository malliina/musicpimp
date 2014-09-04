package com.mle.messaging

import com.mle.messaging.PushConstants._
import com.mle.messaging.gcm.AndroidDevice
import com.ning.http.client.Response

import scala.concurrent.Future

/**
 * @author Michael
 */
trait AndroidMessagingClient extends MessagingClient[AndroidDevice] {
  def send(id: String, data: Map[String, String]): Future[Response]

  def send(dest: AndroidDevice): Future[Response] = send(dest.id, Map(CMD -> STOP, TAG -> dest.tag))
}
