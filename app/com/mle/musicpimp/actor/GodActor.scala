package com.mle.musicpimp.actor

import com.mle.actor.MessageTypes
import controllers.ServerWS._
import com.mle.actor.Messages.Stop
import com.mle.musicpimp.actor.Messages.StartIfStopped

/**
 * @author Michael
 */
class GodActor(messages: MessageTypes[Client])
  extends UserAwareKingActor(messages)
  with ConnectionCountMonitoring[Client] {

  def onZeroConnections() {
    ServerPlayerManager.playbackPoller ! Stop
  }

  def onConnections() {
    ServerPlayerManager.playbackPoller ! StartIfStopped
  }
}
