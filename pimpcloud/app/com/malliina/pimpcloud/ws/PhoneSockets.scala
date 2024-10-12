package com.malliina.pimpcloud.ws

import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import org.apache.pekko.pattern.pipe
import com.malliina.musicpimp.audio.WelcomeMessage
import com.malliina.musicpimp.json.PimpStrings.StatusKey
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.json.JsonStrings.*
import com.malliina.pimpcloud.ws.PhoneMediator.PhoneJoined
import com.malliina.pimpcloud.ws.ServerMediator.{Listen, ServerEvent}
import com.malliina.pimpcloud.{PimpPhone, PimpPhones}
import com.malliina.play.ws.{ActorConfig, JsonActor}
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import play.api.Logger
import play.api.mvc.RequestHeader

/** A mobile client connected to pimpcloud.
  */
class PhoneActor(mediator: ActorRef, conf: ActorConfig[PhoneConnection]) extends JsonActor(conf):
  val conn = conf.user
  val user = conn.user
  val server = conn.server
  val endpoint = PhoneEndpoint(server.id, conf.rh, out)

  override def preStart(): Unit =
    super.preStart()
    mediator ! PhoneJoined(endpoint)
    sendOut(WelcomeMessage)

  override def onMessage(msg: Json): Unit =
    val isStatus = msg.hcursor.downField(Cmd).as[String].contains(StatusKey)
    if isStatus then
      conn
        .status()
        .pipeTo(out)
        .recover:
          case t => PhoneActor.log.warn("Status request failed.", t)
    else
      val payload = Json.obj(
        Cmd -> JsonStrings.Player.asJson,
        Body -> msg,
        UsernameKey -> conn.user.asJson
      )
      server.jsonOut ! payload

object PhoneActor:
  private val log = Logger(getClass)

class PhoneMediator extends Actor:
  var phones: Set[PhoneEndpoint] = Set.empty
  var listeners: Set[ActorRef] = Set.empty

  def phonesJson =
    val ps = phones map { phone =>
      PimpPhone(phone.server, phone.rh.remoteAddress)
    }
    PimpPhones(ps.toSeq)

  override def receive: Receive = {
    // TODO phones should register with their server directly instead
    case ServerEvent(message, server) =>
      phones.filter(_.server == server) foreach { phone =>
        phone.out ! message
      }
    case Listen(listener) =>
      context watch listener
      listeners += listener
      listener ! phonesJson.asJson
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

  def sendUpdate[C: Encoder](message: C): Unit =
    listeners foreach { listener =>
      listener ! message.asJson
    }

object PhoneMediator:

  case class PhoneJoined(endpoint: PhoneEndpoint)

class NoopActor extends Actor:
  override def receive: Receive = { case _ =>
    ()
  }

object NoopActor:
  def props() = Props(new NoopActor)

case class PhoneEndpoint(server: CloudID, rh: RequestHeader, out: ActorRef)
