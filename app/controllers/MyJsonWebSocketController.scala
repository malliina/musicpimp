package controllers

import com.mle.musicpimp.audio.JsonHandlerBase
import com.mle.musicpimp.json.JsonStrings._
import com.mle.play.http.RequestInfo
import play.api.libs.json.{JsObject, JsValue, Json}

/**
 *
 * @author mle
 */
trait MyJsonWebSocketController extends PimpSocket {
  def messageHandler: JsonHandlerBase

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client) {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case STATUS =>
        log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
        val statusJson = status(client)
        val event = Json.obj(EVENT -> STATUS) ++ statusJson.as[JsObject]
        client.channel push event
      case anythingElse =>
        handleMessage(msg, client)
    })
  }

  def handleMessage(message: Message, client: Client): Unit = {
    messageHandler.onJson(message, RequestInfo(client.user, client.request))
  }
}