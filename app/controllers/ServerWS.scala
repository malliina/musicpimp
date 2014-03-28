package controllers

import com.mle.musicpimp.actor.ServerPlayerManager
import com.mle.musicpimp.audio._
import com.mle.util.Log
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonFormats
import play.api.mvc.Call

/**
 *
 * @author mle
 */
trait ServerWS extends ActorJsonWebSocketController with Log {
  override val actorManager = ServerPlayerManager
  override val messageHandler: JsonHandlerBase = PlaybackMessageHandler

  override def status(client: Client) = client.apiVersion match {
    case JsonFormats.JSONv17 => toJson(MusicPlayer.status17)
    case _ => toJson(MusicPlayer.status)
  }

  def subscribeCall: Call = routes.ServerWS.subscribe()
}

object ServerWS extends ServerWS