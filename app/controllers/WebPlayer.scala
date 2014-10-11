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
object WebPlayer extends PlayerSockets with Log {
  override val messageHandler: JsonHandlerBase = WebPlayerMessageHandler

  def status(client: Client): JsValue = {
    val player = WebPlayback.player(client.user)
    PimpRequest.apiVersion(client.request) match {
      case JsonFormatVersions.JSONv17 => player.statusEvent17
      case _ => player.statusEvent
    }
  }

  def openSocketCall: Call = routes.WebPlayer.openSocket()

  def unicast(user: String, json: JsValue) = clients.filter(_.user == user).foreach(_.channel push json)
}