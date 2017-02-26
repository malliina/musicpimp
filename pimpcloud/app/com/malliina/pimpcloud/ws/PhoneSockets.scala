package com.malliina.pimpcloud.ws

import akka.actor.{Actor, ActorRef, Terminated}
import akka.pattern.pipe
import akka.stream.scaladsl.SourceQueue
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.ws.PhoneMediator.PhoneJoined
import com.malliina.play.ws.{ActorConfig, JsonActor, SocketClient}
import controllers.pimpcloud.PhoneConnection
import controllers.pimpcloud.ServerMediator.{Listen, ServerEvent}
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext

class PhoneActor(mediator: ActorRef, conf: ActorConfig[PhoneConnection])(implicit ec: ExecutionContext)
  extends JsonActor(conf) {
  val conn = conf.user
  val user = conn.user
  val server = conn.server
  val endpoint = PhoneEndpoint(conf.user.server.id, conf.rh, out)

  override def preStart() = {
    self ! com.malliina.play.json.JsonMessages.welcome
    mediator ! PhoneJoined(endpoint)
  }

  override def onMessage(msg: JsValue) = {
    val isStatus = (msg \ Cmd).validate[String].filter(_ == StatusKey).isSuccess
    if (isStatus) {
      server.status
        .pipeTo(out)
        .recoverAll(t => log.warning(s"Status request failed.", t))
    } else {
      val payload = Json.obj(
        Cmd -> JsonStrings.Player,
        Body -> msg,
        UsernameKey -> conn.user)
      server.jsonOut ! payload
    }
  }
}

class PhoneMediator extends Actor {
  var phones: Set[PhoneEndpoint] = Set.empty
  var listeners: Set[ActorRef] = Set.empty
  implicit val writer = Writes[PhoneEndpoint](o => Json.obj(
    ServerKey -> o.server,
    Address -> o.rh.remoteAddress
  ))

  def phonesJson = Json.obj(Event -> PhonesKey, Body -> phones)

  override def receive: Receive = {
    // TODO phones should register with their server directly instead
    case ServerEvent(message, server) =>
      phones.filter(_.server == server) foreach { phone =>
        phone.out ! message
      }
    case Listen(listener) =>
      context watch listener
      listeners += listener
      listener ! phonesJson
    case PhoneJoined(endpoint) =>
      context watch endpoint.out
      phones += endpoint
      sendUpdate(phonesJson)
    case Terminated(out) =>
      phones.find(_.out == out) foreach { phone =>
        phones -= phone
      }
      listeners.find(_ == out) foreach { listener =>
        listeners -= listener
      }
      sendUpdate(phonesJson)
  }

  def sendUpdate(message: JsValue) = listeners foreach { listener => listener ! message }
}

object PhoneMediator {

  case class PhoneJoined(endpoint: PhoneEndpoint)

}

case class PhoneEndpoint(server: CloudID, rh: RequestHeader, out: ActorRef)

object PhoneSockets {
  private val log = Logger(getClass)
}

case class PhoneClient(connection: PhoneConnection, channel: SourceQueue[JsValue], req: RequestHeader)
  extends SocketClient[JsValue] {
  val phoneUser = connection.user
  val connectedServer = connection.server
}
