package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.{AuthedRequest, FullUrls}
import com.malliina.play.ws._
import play.api.libs.json.JsValue
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
  val cloudWriter = ServerMessage.jsonWriter(TrackJson.format(clouds.cloudHost))
  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new PlayerActor(MusicPlayer, handler, conf))
  }

  def sendToPimpcloud(message: ServerMessage) = {
    clouds.sendIfConnected(cloudWriter writes message)
  }

  def openSocket = sockets.newSocket
}

class PlayerActor(player: ServerPlayer,
                  messageHandler: JsonHandlerBase,
                  conf: ActorConfig[AuthedRequest]) extends JsonActor(conf) {
  val ticks = Observable.interval(900.millis)
  val messageWriter = ServerMessage.jsonWriter(TrackJson.format(FullUrls.hostOnly(rh)))
  val apiVersion = PimpRequest.apiVersion(rh)
  implicit val w = TrackJson.writer(rh)
  val user = conf.user.user
  var eventSub: Option[Subscription] = None
  var previousPos = -1L
  val remoteInfo = RemoteInfo(user, PimpRequest.apiVersion(rh), FullUrls.hostOnly(rh), Target(json => out ! json))

  override def preStart(): Unit = {
    super.preStart()
    sendOut(WelcomeMessage)
    val playbackEvents = player.allEvents.subscribe(
      event => out ! messageWriter.writes(event),
      _ => (),
      () => ()
    )
    val timeUpdates = ticks.subscribe(_ => onTick())
    eventSub = Option(CompositeSubscription(playbackEvents, timeUpdates))
  }

  override def onMessage(msg: JsValue): Unit =
    messageHandler.onJson(msg, remoteInfo)

  def onTick() {
    val pos = player.position
    val posSeconds = pos.toSeconds
    if (posSeconds != previousPos) {
      sendOut(TimeUpdatedMessage(pos))
      previousPos = posSeconds
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    eventSub foreach { sub => sub.unsubscribe() }
  }
}
