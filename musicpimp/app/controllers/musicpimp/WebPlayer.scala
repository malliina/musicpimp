package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages, JsonStrings}
import com.malliina.musicpimp.models.{FullUrl, RemoteInfo}
import com.malliina.play.ActorExecution
import com.malliina.play.http.{AuthedRequest, RequestInfo}
import com.malliina.play.ws.{ActorConfig, ActorMeta, JsonActor, Sockets}
import play.api.Logger
import play.api.libs.json.JsValue

class WebPlayer(ctx: ActorExecution) {
  val sockets = new Sockets(Auths.session, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]): Props = {
      val remote = RemoteInfo(conf.user.user, FullUrl.hostOnly(conf.rh))
      Props(new WebPlayActor(remote, conf))
    }
  }

  def openSocket = sockets.newSocket
}

trait Target {
  def send(json: JsValue): Unit
}

class WebPlayActor(remote: RemoteInfo, ctx: ActorMeta) extends JsonActor(ctx) {
  val user = remote.user
  val target = new Target {
    override def send(json: JsValue): Unit = out ! json
  }
  val player = new PimpWebPlayer(remote, target)
  val handler = new WebPlayerMessageHandler(remote, player)
  val requestInfo = RequestInfo(user, rh)

  override def preStart() = {
    out ! com.malliina.play.json.JsonMessages.welcome
  }

  override def onMessage(msg: JsValue) = {
    (msg \ JsonStrings.Cmd).asOpt[String].fold(log warning s"Unknown message: $msg")({
      case JsonStrings.StatusKey =>
        log info s"User '$user' from '$address' said '$msg'."
        val event = JsonMessages.withStatus(statusEvent())
        out ! event
      case _ =>
        handler.onJson(msg, requestInfo)
    })
  }

  def statusEvent() = {
    PimpRequest.apiVersion(rh) match {
      case JsonFormatVersions.JSONv17 => player.statusEvent17
      case _ => player.statusEvent
    }
  }
}

object WebPlayer {
  private val log = Logger(getClass)
}
