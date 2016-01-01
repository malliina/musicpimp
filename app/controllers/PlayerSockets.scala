package controllers

import com.malliina.musicpimp.audio.JsonHandlerBase
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.play.Authenticator
import com.malliina.play.http.RequestInfo
import controllers.PlayerSockets.log
import play.api.Logger
import play.api.libs.json.JsValue
/**
 *
 * @author mle
 */
abstract class PlayerSockets(auth: Authenticator) extends PimpSockets(auth) {
  def messageHandler: JsonHandlerBase

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client): Boolean = {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case STATUS =>
        log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
        val event = JsonMessages.withStatus(status(client)) // Json.obj(EVENT -> STATUS) ++ statusJson.as[JsObject]
        client.channel push event
      case anythingElse =>
        handleMessage(msg, client)
    })
    true
  }

  def handleMessage(message: Message, client: Client): Unit = {
    messageHandler.onJson(message, RequestInfo(client.user, client.request))
  }
}

object PlayerSockets {
  private val log = Logger(getClass)
}
