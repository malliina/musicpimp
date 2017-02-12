package com.malliina.ws

trait SocketStorage[C] {
  def clients: Seq[C]

  def onConnect(c: C): Unit

  def onDisconnect(c: C): Unit
}
