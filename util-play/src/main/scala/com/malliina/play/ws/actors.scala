package com.malliina.play.ws

import org.apache.pekko.actor.{ActorRef, Props}
import com.malliina.collections.BoundedList
import com.malliina.play.ws.Mediator.Broadcast
import io.circe.Json
import play.api.mvc.RequestHeader

case class MediatorClient(ctx: ActorMeta, mediator: ActorRef) extends ClientContext:
  override def out = ctx.out
  override def rh = ctx.rh

trait ClientContext extends ActorMeta:
  def mediator: ActorRef

case class ActorInfo(out: ActorRef, rh: RequestHeader) extends ActorMeta

trait ActorMeta:
  def out: ActorRef
  def rh: RequestHeader

case class DefaultActorConfig[U](out: ActorRef, rh: RequestHeader, user: U) extends ActorConfig[U]

trait ActorConfig[U] extends ActorMeta:
  def user: U

class ReplayMediator(bufferSize: Int) extends Mediator:
  val broadcastHistory = BoundedList.empty[Json](bufferSize)

  override def onJoined(ref: ActorRef): Unit =
    broadcastHistory.foreach: json =>
      ref ! json

  override def onBroadcast(message: Json): Unit =
    broadcastHistory += message

class ForwardingMediator(sink: ActorRef) extends Mediator:
  override def onClientMessage(message: Json, rh: RequestHeader): Unit =
    sink ! Broadcast(message)

object ForwardingMediator:
  def props(sink: ActorRef) = Props(new ForwardingMediator(sink))

class SelfMediator extends Mediator:
  override def onClientMessage(message: Json, rh: RequestHeader): Unit =
    self ! Broadcast(message)

object SelfMediator:
  def props() = Props(new SelfMediator)
