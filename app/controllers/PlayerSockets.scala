package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.audio.JsonHandlerBase
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.play.Authenticator
import com.malliina.play.http.RequestInfo
import com.malliina.play.models.Username
import controllers.PlayerSockets.log
import play.api.Logger
import play.api.libs.json.JsValue

abstract class PlayerSockets(auth: Authenticator, mat: Materializer)
  extends PimpSockets(auth, mat) {

  def messageHandler: JsonHandlerBase

  def status(client: Client): JsValue

  override def onMessage(msg: Message, client: Client): Boolean = {
    (msg \ Cmd).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case JsonStrings.Status =>
        log info s"User: ${client.user} from: ${client.remoteAddress} said: $msg"
        val event = JsonMessages.withStatus(status(client))
        client.channel offer event
      case anythingElse =>
        handleMessage(msg, client)
    })
    true
  }

  def handleMessage(message: Message, client: Client): Unit =
    messageHandler.onJson(message, RequestInfo[Username](client.user, client.request))
}

object PlayerSockets {
  private val log = Logger(getClass)
}
