package com.mle.musicpimp.actor

import com.mle.actor.ActorManager
import controllers.ServerWS.Client
import scala.concurrent._
import scala.concurrent.duration._
import com.mle.util.Log
import akka.pattern.gracefulStop
import com.mle.play.concurrent.ExecutionContexts.synchronousIO

/**
 * @author Michael
 */
object ServerPlayerManager extends ActorManager[Client] with ActorManagement with Log {
  val king = newActor(new GodActor(messages))
  val playbackPoller = newActor(new PlaybackUpdater)

  def shutdown() {
    val f = gracefulStop(king, 5 seconds)
    val f2 = gracefulStop(playbackPoller, 5 seconds)
    val stopTasks = Future.sequence(Seq(f, f2))
    Await.result(stopTasks, 6 seconds)
    actorSystem.shutdown()
  }
}

