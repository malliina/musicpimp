package com.mle.musicpimp.actor

import com.mle.actor.ActorManager
import controllers.WebPlayerController.Client
import scala.concurrent._
import scala.concurrent.duration._
import akka.pattern.gracefulStop

/**
 *
 * @author mle
 */
object WebPlayerManager extends ActorManager[Client] with ActorManagement {
  val king = newActor(new UserAwareKingActor(messages))

  def shutdown() {
    val kingStopper = gracefulStop(king, 5 seconds)
    Await.result(kingStopper, 6 seconds)
    actorSystem.shutdown()
  }
}
