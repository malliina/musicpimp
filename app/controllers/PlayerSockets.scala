package controllers

import com.mle.musicpimp.audio.JsonHandlerBase
import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.json.JsonStrings._
import com.mle.play.http.RequestInfo
import play.api.libs.json.JsValue

/**
 *
 * @author mle
 */
trait PlayerSockets extends PimpSockets {
  def messageHandler: JsonHandlerBase

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client) {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case STATUS =>
        log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
        val event = JsonMessages.withStatus(status(client)) // Json.obj(EVENT -> STATUS) ++ statusJson.as[JsObject]
        client.channel push event
      case anythingElse =>
        handleMessage(msg, client)
    })
  }

  def handleMessage(message: Message, client: Client): Unit = {
    messageHandler.onJson(message, RequestInfo(client.user, client.request))
  }
}
