package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages, JsonStrings, Target}
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.ActorExecution
import com.malliina.play.http.{AuthedRequest, FullUrls}
import com.malliina.play.models.Username
import com.malliina.play.ws.{ActorConfig, ActorMeta, JsonActor, Sockets}
import controllers.musicpimp.WebPlayActor.log
import play.api.Logger
import play.api.libs.json.JsValue

class WebPlayer(ctx: ActorExecution) {
  val sockets = new Sockets(Auths.session, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]): Props = {
      val remote = RemoteInfo(conf.user.user, FullUrls.hostOnly(conf.rh))
      Props(new WebPlayActor(remote, conf))
    }
  }

  def openSocket = sockets.newSocket
}

object WebPlayer {
  private val log = Logger(getClass)
}

class WebPlayActor(remote: RemoteInfo, ctx: ActorMeta)
  extends JsonActor(ctx) {
  val user: Username = remote.user
  val target = Target(json => out ! json)
  val player = new PimpWebPlayer(remote, target)
  val handler = new WebPlayerMessageHandler(remote, player)

  override def preStart() = {
    super.preStart()
    out ! com.malliina.play.json.JsonMessages.welcome
  }

  override def onMessage(msg: JsValue) = {
    (msg \ JsonStrings.Cmd).asOpt[String].fold(log warn s"Unknown message '$msg'.")({
      case JsonStrings.StatusKey =>
        log info s"User '$user' from '$address' said '$msg'."
        val event = JsonMessages.withStatus(statusEvent())
        out ! event
      case _ =>
        handler.onJson(msg, user, rh)
    })
  }

  def statusEvent() = {
    PimpRequest.apiVersion(rh) match {
      case JsonFormatVersions.JSONv17 => player.statusEvent17
      case _ => player.statusEvent
    }
  }
}

object WebPlayActor {
  private val log = Logger(getClass)
}
