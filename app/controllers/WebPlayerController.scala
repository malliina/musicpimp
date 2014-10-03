package controllers

import com.mle.musicpimp.audio.{JsonHandlerBase, WebPlayback, WebPlayerMessageHandler}
import com.mle.musicpimp.json.JsonFormatVersions
import com.mle.util.Log
import play.api.libs.json.JsValue
import play.api.mvc.Call

/**
 *
 * @author mle
 */
trait WebPlayerController extends MyJsonWebSocketController with Log {
  override val messageHandler: JsonHandlerBase = WebPlayerMessageHandler

  def status(client: Client): JsValue = {
    val player = WebPlayback.player(client.user)
    PimpRequest.apiVersion(client.request) match {
      case JsonFormatVersions.JSONv17 => player.statusEvent17
      case _ => player.statusEvent
    }
  }

  def openSocketCall: Call = routes.WebPlayerController.openSocket()
}

object WebPlayerController extends WebPlayerController
