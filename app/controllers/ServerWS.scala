package controllers

import com.mle.musicpimp.audio._
import com.mle.musicpimp.json.JsonFormats
import com.mle.util.Log
import play.api.libs.json.Json.toJson
import play.api.mvc.Call

/**
 *
 * @author mle
 */
trait ServerWS extends ActorJsonWebSocketController with Log {
  //  override val actorManager = ServerPlayerManager
  override val messageHandler: JsonHandlerBase = PlaybackMessageHandler

  override def status(client: Client) = apiVersion(client) match {
    case JsonFormats.JSONv17 => toJson(MusicPlayer.status17)
    case _ => toJson(MusicPlayer.status)
  }

  def apiVersion(client: Client): String = PimpRequest.apiVersion(client.request)

  def openSocketCall: Call = routes.ServerWS.openSocket()
}

object ServerWS extends ServerWS