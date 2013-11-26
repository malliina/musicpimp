package com.mle.musicpimp.actor

import akka.actor.{ActorDSL, Actor, ActorSystem}
import com.mle.util.Util

/**
 *
 * @author mle
 */
trait ActorManagement {
  implicit val actorSystem = ActorSystem("actor-system")
  Util.addShutdownHook(actorSystem.shutdown())

  def newActor(actor: => Actor) = ActorDSL.actor(actorSystem)(actor)

}
