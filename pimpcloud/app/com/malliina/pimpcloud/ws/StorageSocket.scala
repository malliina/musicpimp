package com.malliina.pimpcloud.ws

import com.malliina.play.ws.SyncSockets
import com.malliina.ws.SocketStorage

trait StorageSocket extends SyncSockets {
  def storage: SocketStorage[Client]

  override def clientsSync: Seq[Client] =
    storage.clients

  override def onConnectSync(client: Client): Unit =
    storage.onConnect(client)

  override def onDisconnectSync(client: Client): Unit =
    storage.onDisconnect(client)
}
