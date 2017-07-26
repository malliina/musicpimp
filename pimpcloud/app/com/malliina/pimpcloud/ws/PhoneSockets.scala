package com.malliina.pimpcloud.ws

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.pattern.pipe
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models.{PimpPhone, PimpPhones}
import com.malliina.pimpcloud.ws.PhoneMediator.PhoneJoined
import com.malliina.play.ws.{ActorConfig, JsonActor}
import controllers.pimpcloud.ServerMediator.{Listen, ServerEvent}
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.RequestHeader

class PhoneActor(mediator: ActorRef, conf: ActorConfig[PhoneConnection])
  extends JsonActor(conf) {
  val conn = conf.user
  val user = conn.user
  val server = conn.server
  val endpoint = PhoneEndpoint(server.id, conf.rh, out)

  override def preStart() = {
    super.preStart()
    mediator ! PhoneJoined(endpoint)
    out ! com.malliina.play.json.JsonMessages.welcome
  }

  override def onMessage(msg: JsValue) = {
    val isStatus = (msg \ Cmd).validate[String].filter(_ == StatusKey).isSuccess
    if (isStatus) {
      conn.status()
        .pipeTo(out)
        .recoverAll(t => PhoneActor.log.warn("Status request failed.", t))
    } else {
      val payload = Json.obj(
        Cmd -> JsonStrings.Player,
        Body -> msg,
        UsernameKey -> conn.user
      )
      server.jsonOut ! payload
    }
  }
}

object PhoneActor {
  private val log = Logger(getClass)
}

class PhoneMediator extends Actor {
  var phones: Set[PhoneEndpoint] = Set.empty
  var listeners: Set[ActorRef] = Set.empty

  def phonesJson = {
    val ps = phones map { phone =>
      PimpPhone(phone.server, phone.rh.remoteAddress)
    }
    PimpPhones(ps.toSeq)
  }

  override def receive: Receive = {
    // TODO phones should register with their server directly instead
    case ServerEvent(message, server) =>
      phones.filter(_.server == server) foreach { phone =>
        phone.out ! message
      }
    case Listen(listener) =>
      context watch listener
      listeners += listener
      listener ! Json.toJson(phonesJson)
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

  def sendUpdate[C: Writes](message: C) = {
    val json = Json.toJson(message)
    listeners foreach { listener =>
      listener ! json
    }
  }
}

object PhoneMediator {

  case class PhoneJoined(endpoint: PhoneEndpoint)

}

class NoopActor extends Actor {
  override def receive: Receive = {
    case _ => ()
  }
}

object NoopActor {
  def props() = Props(new NoopActor)
}

case class PhoneEndpoint(server: CloudID, rh: RequestHeader, out: ActorRef)
