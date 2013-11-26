package com.mle.musicpimp.actor

import com.mle.actor.{KingActor, MessageTypes}
import controllers.ServerWS._
import play.api.libs.json.JsValue
import com.mle.musicpimp.actor.Messages.{ChannelJson, UserJson}
import com.mle.util.Log

/**
 *
 * @author mle
 */
class UserAwareKingActor(messages: MessageTypes[Client])
  extends KingActor[Client](messages) with Log {
  val clientActorBuilder = (client: Client) =>
    ServerPlayerManager.newActor(new WebSocketsClient(client))

  /**
   * Sends JSON messages to clients. Three modes are supported:
   *
   * 1) broadcast: send to all clients
   * 2) user: send to all clients logged in as user X (multicast)
   * 3) channel: send to one specific client (unicast)
   */
  override def messageHandler = {
    case broadcastJson: JsValue =>
      connections.foreach(_.actor ! broadcastJson)
    case userJson: UserJson =>
      connections.filter(_.address.user == userJson.user)
        .foreach(_.actor ! userJson.json)
    case channelJson: ChannelJson =>
      connections.filter(_.address.channel == channelJson.channel)
        .foreach(_.actor ! channelJson.json)
  }
}

