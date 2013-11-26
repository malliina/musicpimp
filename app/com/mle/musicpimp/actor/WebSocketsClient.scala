package com.mle.musicpimp.actor

import controllers.ServerWS._
import akka.actor.Actor
import com.mle.actor.Messages.Stop
import play.api.libs.json.JsValue
import com.mle.util.Log

/**
 * @author Michael
 */
class WebSocketsClient(client: Client) extends Actor with Log {
  def receive = {
    case json: JsValue =>
      client.channel.push(json)
    case Stop =>
      context.stop(self)
  }
}
