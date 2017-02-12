package com.malliina.pimpcloud.actors

import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

/** Manager of phone websockets.
  */
class PhonesActor extends ItemsActor[PhoneClient] {
  override def receive: Receive = {
    case PhonesActor.Connect(client) =>
      clients += client
      sender() ! clients
    case PhonesActor.Disconnect(client) =>
      clients -= client
      sender() ! clients
    case PhonesActor.Message(message, recipient) =>
      clients.find(_ == recipient).foreach(_.channel offer message)
    case PhonesActor.MessageFromServer(message, server) =>
      clients.filter(_.connectedServer == server).foreach(_.channel offer message)
    case PhonesActor.Broadcast(message) =>
      clients.foreach(_.channel offer message)
    case PhonesActor.GetClients =>
      sender() ! clients
  }
}

object PhonesActor extends MessagesBase[JsValue, PhoneClient] {

  case class MessageFromServer(message: JsValue, server: PimpServerSocket)

}
