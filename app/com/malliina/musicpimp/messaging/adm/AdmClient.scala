package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.push.MessagingClient
import com.malliina.push.adm.ADMClient
import com.ning.http.client.Response

import scala.concurrent.Future

/**
  * What can an attacker do with this information?
  *
  * @author Michael
  */
object AdmClient
  extends ADMClient(
    clientID = "amzn1.application-oa2-client.08957c08d1754dc2bf963d7c265f6c4b",
    clientSecret = "65d6d9e6dffb7bc5e452fcfc84d5cb77e2ddc8f07592e944a2b2630d6653fdf4")
  with MessagingClient[ADMDevice] {

  override def send(dest: ADMDevice): Future[Response] =
    send(dest.id, Map(Cmd -> Stop, Tag -> dest.tag.tag))
}
