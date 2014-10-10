package com.mle.ws

import scala.concurrent.Future


/**
 *
 * @tparam T type of message sent over the websocket connection
 */
trait WebSocketBase[T] {
  /**
   * @return a future that completes when the connection has successfully been established
   */
  def connect(): Future[Unit]

  def send(json: T): Unit

  def onMessage(json: T): Unit

  def onClose(): Unit

  def onError(t: Exception): Unit

  def close(): Unit
}
