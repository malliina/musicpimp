package com.malliina.pimpcloud.actors

import akka.actor.ActorLogging
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import play.api.libs.json.JsValue

/** Manager of server websockets.
  */
class ServersActor extends ItemsActor[PimpServerSocket] with ActorLogging {
  override def receive: Receive = {
    case ServersActor.Connect(client) =>
      clients += client
      logEvent(client.id, "connected")
      sender() ! ServersActor.Clients(clients)
    case ServersActor.Disconnect(client) =>
      clients -= client
      logEvent(client.id, "disconnected")
      sender() ! ServersActor.Clients(clients)
    case ServersActor.Message(json, recipient) =>
      clients.find(_ == recipient).foreach(_.jsonOut ! json)
    case ServersActor.Broadcast(json) =>
      clients.foreach(_.jsonOut ! json)
    case ServersActor.GetClients =>
      sender() ! ServersActor.Clients(clients)
  }

  def logEvent(id: CloudID, action: String) =
    log info s"MusicPimp client $action: $id. Clients connected: ${clients.size}"
}

object ServersActor extends MessagesBase[JsValue, PimpServerSocket]
