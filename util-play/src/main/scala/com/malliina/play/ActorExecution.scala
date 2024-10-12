package com.malliina.play

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer

case class ActorExecution(actorSystem: ActorSystem, materializer: Materializer):
  val executionContext = materializer.executionContext
