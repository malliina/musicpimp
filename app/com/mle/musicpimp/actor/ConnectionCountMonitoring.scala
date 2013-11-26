package com.mle.musicpimp.actor

import com.mle.actor.KingActor

/**
 * @author Michael
 */
trait ConnectionCountMonitoring[T] extends KingActor[T] {
  override protected def onConnect(clientAddress: T): Int = {
    val connections = super.onConnect(clientAddress)
    if (connections == 1) {
      onConnections()
    }
    connections
  }

  override protected def onDisconnect(clientAddress: T): Int = {
    val connections = super.onDisconnect(clientAddress)
    if (connections == 0) {
      onZeroConnections()
    }
    connections
  }

  def onZeroConnections()

  def onConnections()
}
