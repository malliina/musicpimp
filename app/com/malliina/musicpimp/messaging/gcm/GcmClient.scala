package com.malliina.musicpimp.messaging.gcm

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.push.MessagingClient
import com.malliina.push.gcm.GCMClient
import com.ning.http.client.Response

import scala.concurrent.Future

/**
  * What can a rogue actor do with the api key?
  *
  * @author mle
  */
object GcmClient
  extends GCMClient(apiKey = "AIzaSyCCDniLRhlHAfnXIJnsVn-You2QQKLfrM8")
  with MessagingClient[GCMDevice] {

  override def send(dest: GCMDevice): Future[Response] =
    send(dest.id, Map(Cmd -> Stop, Tag -> dest.tag.tag))
}
