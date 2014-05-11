package controllers

import com.mle.actor.ActorManager
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsObject, Json, JsValue}
import com.mle.musicpimp.actor.Messages.ChannelJson
import com.mle.play.json.JsonMessages
import com.mle.musicpimp.audio.JsonHandlerBase
import com.mle.play.RequestInfo

/**
 *
 * @author mle
 */
trait ActorJsonWebSocketController extends JsonWebSocketController {
  override val welcomeMessage: Option[Message] = Some(JsonMessages.welcome)

  def messageHandler: JsonHandlerBase

  def actorManager: ActorManager[Client]

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client) {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case STATUS =>
        log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
        val statusJson = status(client)
        val event = Json.obj(EVENT -> STATUS) ++ statusJson.as[JsObject]
        actorManager.king ! ChannelJson(client.channel, event)
      case anythingElse =>
        handleMessage(msg, client)
    })
  }

  def handleMessage(message: Message, client: Client): Unit = {
    messageHandler.onJson(message, RequestInfo(client.user, client.request))
  }

  def onConnect(client: Client): Unit = actorManager connect client

  def onDisconnect(client: Client) = actorManager disconnect client
}
