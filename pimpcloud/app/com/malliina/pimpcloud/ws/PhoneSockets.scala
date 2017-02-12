package com.malliina.pimpcloud.ws

import akka.stream.{Materializer, QueueOfferResult}
import akka.stream.scaladsl.SourceQueue
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.pimpcloud.ws.PhoneSockets.log
import com.malliina.play.ws.SocketClient
import com.malliina.ws.{PhoneActorSockets, RxStmStorage}
import controllers.PhoneConnection
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.{Call, RequestHeader}
import rx.lang.scala.Observable

import scala.concurrent.Future

abstract class PhoneSockets(val storage: RxStmStorage[PhoneClient], val mat: Materializer)
  extends PhoneActorSockets(mat) {
  override type Client = PhoneClient
  override type AuthSuccess = PhoneConnection

  implicit val writer = Writes[PhoneClient](o => Json.obj(
    ServerKey -> o.connectedServer.id,
    Address -> o.req.remoteAddress
  ))

  val usersJson: Observable[JsObject] =
    storage.users.map(phoneClients => Json.obj(Event -> PhonesKey, Body -> phoneClients))

  def authenticatePhone(req: RequestHeader): Future[AuthSuccess]

  // TODO this is shit; cannot concurrently offer
  def send(message: Message, from: PimpServerSocket): Future[Seq[QueueOfferResult]] =
    clients.flatMap(cs => Future.traverse(cs.filter(_.connectedServer == from))(_.channel.offer(message)))

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = authenticatePhone(req)

  override def newClient(authResult: PhoneConnection, channel: SourceQueue[JsValue], request: RequestHeader): PhoneClient =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: PhoneClient): Boolean = {
    val isStatus = (msg \ Cmd).validate[String].filter(_ == StatusKey).isSuccess
    if (isStatus) {
      client.connectedServer.status
        .flatMap(resp => client.channel offer resp)
        .recoverAll(t => log.warn(s"Status request failed.", t))
    } else {
      val payload = Json.obj(
        Cmd -> JsonStrings.Player,
        Body -> msg,
        UsernameKey -> client.phoneUser.name)
      client.connectedServer send payload
    }
    true
  }

  override def welcomeMessage(client: Client): Option[JsValue] =
    Some(com.malliina.play.json.JsonMessages.welcome)
}

object PhoneSockets {
  private val log = Logger(getClass)
}

case class PhoneClient(connection: PhoneConnection, channel: SourceQueue[JsValue], req: RequestHeader)
  extends SocketClient[JsValue] {
  val phoneUser = connection.user
  val connectedServer = connection.server
}
