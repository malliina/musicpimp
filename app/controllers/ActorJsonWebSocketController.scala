package controllers

import com.mle.actor.ActorManager
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsObject, Json, JsValue}
import com.mle.musicpimp.actor.Messages.ChannelJson
import com.mle.play.json.JsonMessages

/**
 *
 * @author mle
 */
trait ActorJsonWebSocketController extends JsonWebSocketController {
  override val welcomeMessage: Option[Message] = Some(JsonMessages.welcome)

  def actorManager: ActorManager[Client]

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client) {
    log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
    (msg \ CMD).asOpt[String].map {
      case STATUS =>
        val statusJson = status(client)
        val event = Json.obj(EVENT -> STATUS) ++ statusJson.as[JsObject]
        actorManager.king ! ChannelJson(client.channel, event)
      case anythingElse =>
        handleMessage(msg, client)
    }.getOrElse(log warn s"Unknown message: $msg")
  }

  def handleMessage(message: Message, client: Client)

  def onConnect(client: Client): Unit = actorManager connect client

  def onDisconnect(client: Client) = actorManager disconnect client
}
