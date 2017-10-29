package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.messaging.cloud.{APNSHttpResult, APNSPayload, APNSResult}
import com.malliina.push.apns._

import scala.concurrent.Future

/** Legacy.
  */
class APNSHandler(client: APNSClient) extends PushRequestHandler[APNSPayload, APNSResult] {
  override def push(requests: Seq[APNSPayload]): Future[Seq[APNSResult]] =
    Future.traverse(requests) { r =>
      client.push(r.token, r.message).map(APNSResult.fromAPNS)
    }

  override def pushOne(r: APNSPayload): Future[APNSResult] =
    client.push(r.token, r.message).map(APNSResult.fromAPNS)
}

/** Using HTTP/2.
  */
class APNSHttpHandler(client: APNSHttpClient) extends PushRequestHandler[APNSPayload, APNSHttpResult] {
  val MusicPimpTopic = APNSTopic("org.musicpimp.MusicPimp")
  val meta = APNSMeta.withTopic(MusicPimpTopic)

  def pushOne(request: APNSPayload) = {
    val token = request.token
    client.push(token, APNSRequest(request.message, meta)).map(_.fold(
      err => APNSHttpResult(token, None, Option(err)),
      id => APNSHttpResult(token, Option(id), None)
    ))
  }

}
