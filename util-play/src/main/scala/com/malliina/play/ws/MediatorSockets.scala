package com.malliina.play.ws

import org.apache.pekko.actor.Props
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator

/** Manages websockets.
  *
  * To send a message to all connected clients: `mediator ! Broadcast(myJsonMessage)`
  *
  * The actor created from `mediatorProps` will receive any messages sent from connected websockets.
  */
class MediatorSockets[User](mediatorProps: Props, auth: Authenticator[User], ctx: ActorExecution)
  extends Sockets[User](auth, ctx):
  val mediator = actorSystem.actorOf(mediatorProps)

  override def props(conf: ActorConfig[User]): Props =
    ClientActor.props(MediatorClient(conf, mediator))
