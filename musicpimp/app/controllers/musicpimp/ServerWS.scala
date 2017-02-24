package controllers.musicpimp

import akka.actor.{ActorRef, Props}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages}
import com.malliina.musicpimp.models.FullUrl
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.{AuthedRequest, RequestInfo}
import com.malliina.play.models.Username
import com.malliina.play.ws._
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.RequestHeader
import rx.lang.scala.subscriptions.CompositeSubscription
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationInt

/** Emits playback events to and accepts commands from listening clients.
  */
class ServerWS(val clouds: Clouds,
               auth: Authenticator[AuthedRequest],
               handler: PlaybackMessageHandler,
               ctx: ActorExecution) {

  val serverMessages = MusicPlayer.allEvents
  val subscription = serverMessages.subscribe(event => sendToPimpcloud(event))
  val cloudWriter = ServerMessage.writer(clouds.cloudHost)

  def sendToPimpcloud(message: ServerMessage) = {
    clouds.sendIfConnected(cloudWriter writes message)
  }

  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new PlayerActor(MusicPlayer, handler, conf))
  }

  def openSocket = sockets.newSocket
}

class PlayerActor(player: ServerPlayer,
                  messageHandler: JsonHandlerBase,
                  conf: ActorConfig[AuthedRequest]) extends JsonActor(conf.rh) {
  val ticks = Observable.interval(900.millis)
  val messageWriter = ServerMessage.writer(FullUrl.hostOnly(rh))
  val apiVersion = PimpRequest.apiVersion(rh)
  implicit val w = TrackJson.writer(rh)
  val out = conf.out
  val user = conf.user.user
  var eventSub: Option[Subscription] = None
  var previousPos = -1L

  def status() = {
    val json = apiVersion match {
      case JsonFormatVersions.JSONv17 => toJson(player.status17)
      case _ => toJson(player.status)
    }
    JsonMessages.withStatus(json)
  }

  override def preStart() = {
    out ! com.malliina.play.json.JsonMessages.welcome
    val playbackEvents = player.allEvents.subscribe(
      event => out ! messageWriter.writes(event),
      _ => (),
      () => ()
    )
    val timeUpdates = ticks.subscribe(_ => onTick())
    eventSub = Option(CompositeSubscription(playbackEvents, timeUpdates))
  }

  override def onMessage(msg: JsValue) = {
    (msg \ Cmd).asOpt[String].fold(log warning s"Unknown message: '$msg'.") {
      case StatusKey =>
        out ! status()
      case _ =>
        messageHandler.onJson(msg, RequestInfo(user, rh))
    }
  }

  def onTick() {
    val pos = player.position
    val posSeconds = pos.toSeconds
    if (posSeconds != previousPos) {
      out ! JsonMessages.timeUpdated(pos)
      previousPos = posSeconds
    }
  }

  override def postStop() = {
    eventSub foreach { sub => sub.unsubscribe() }
  }
}
