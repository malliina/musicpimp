package com.malliina.pimpcloud

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.play.ws.{SocketClient, WebSocketBase}

import scala.concurrent.Future

trait ClientStorage[M, C <: SocketClient[M]] extends WebSocketBase {

  override type Message = M
  override type Client = C

  def storage: AsyncClientHandler[M, C]

  override def clients: Future[Seq[C]] =
    storage.clients.map(_.toSeq)

  override def onConnect(client: C): Future[Unit] =
    storage.onConnect(client)

  override def onDisconnect(client: C): Future[Unit] =
    storage.onDisconnect(client)
}
