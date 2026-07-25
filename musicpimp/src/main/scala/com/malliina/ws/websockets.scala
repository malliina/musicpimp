package com.malliina.ws

import scala.util.Try

class NotConnectedException(msg: String) extends Exception(msg)

/** @tparam T
  *   type of message sent over the websocket connection
  */
trait WebSocketBase[F[_], T]:
  /** @return
    *   a future that completes when the connection has successfully been established
    */
  def connect(): F[Unit]

  def send(json: T): Try[Unit]

  def onMessage(json: T): F[Unit]

  def onClose(): F[Unit]

  def onError(t: Exception): F[Unit]

  def close(): Unit
