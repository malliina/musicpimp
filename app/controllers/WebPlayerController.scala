package controllers

import play.api.mvc.{Call, RequestHeader}
import com.mle.musicpimp.actor.WebPlayerManager
import com.mle.musicpimp.audio.{JsonMessageHandler, WebPlayback}
import com.mle.util.Log
import play.api.libs.json.JsValue
import com.mle.musicpimp.json.JsonFormats

/**
 *
 * @author mle
 */
trait WebPlayerController extends ActorJsonWebSocketController with Log {
  override val actorManager = WebPlayerManager

  def status(client: Client): JsValue = {
    val player = WebPlayback.player(client.user)
    client.apiVersion match {
      case JsonFormats.JSONv17 => player.statusEvent17
      case _ => player.statusEvent
    }
  }

  def handleMessage(message: Message, client: Client) {
    JsonMessageHandler.onClientMessage(client.user, message)
  }

  def subscribeCall: Call = routes.WebPlayerController.subscribe()
}

object WebPlayerController extends WebPlayerController
