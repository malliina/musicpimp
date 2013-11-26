package controllers

import com.mle.musicpimp.actor.ServerPlayerManager
import com.mle.musicpimp.audio.{MusicPlayer, JsonMessageHandler}
import com.mle.util.Log
import play.api.mvc.{Call, RequestHeader}
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonFormats

/**
 *
 * @author mle
 */
trait ServerWS extends ActorJsonWebSocketController with Log {
  override val actorManager = ServerPlayerManager

  override def status(client: Client) = client.apiVersion match {
    case JsonFormats.JSONv17 => toJson(MusicPlayer.status17)
    case _ => toJson(MusicPlayer.status)
  }

  def handleMessage(message: Message, client: Client) {
    JsonMessageHandler.onPlaybackCommand(message)
  }
  def subscribeCall: Call = routes.ServerWS.subscribe()
}

object ServerWS extends ServerWS