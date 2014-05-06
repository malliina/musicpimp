package com.mle.messaging

import scala.concurrent.{ExecutionContext, Future}
import com.ning.http.client.Response
import com.mle.util.Log
import com.mle.concurrent.FutureImplicits.RichFuture

/**
 *
 * @author mle
 */
trait MessagingClient[T] extends Log {
  def send(dest: T): Future[Response]

  def sendLogged(dest: T)(implicit ec: ExecutionContext): Future[Unit] = send(dest)
      .map(r => log info s"Sent message to: $dest. Response: ${r.getStatusText}")
      .recoverAll(t => log.warn(s"Unable to send message to: $dest", t))
}
