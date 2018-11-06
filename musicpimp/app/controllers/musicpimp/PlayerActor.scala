package controllers.musicpimp

import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{AuthedRequest, FullUrls}
import com.malliina.play.ws.{ActorConfig, JsonActor}
import play.api.libs.json.JsValue
import rx.lang.scala.subscriptions.CompositeSubscription
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationInt

class PlayerActor(player: ServerPlayer,
                  messageHandler: JsonHandlerBase,
                  conf: ActorConfig[AuthedRequest]) extends JsonActor(conf) {
  // Keepalive for very old (Android) clients
  val pings = Observable.interval(5.seconds)
  // Playback updates
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
    val pingUpdates = pings.subscribe(_ => onPing())
    eventSub = Option(CompositeSubscription(playbackEvents, timeUpdates, pingUpdates))
  }

  override def onMessage(msg: JsValue): Unit =
    messageHandler.onJson(msg, remoteInfo)


  def onTick(): Unit = {
    val pos = player.position
    val posSeconds = pos.toSeconds
    if (posSeconds != previousPos) {
      sendOut(TimeUpdatedMessage(pos))
      previousPos = posSeconds
    }
  }

  def onPing(): Unit = sendOut(PingEvent)

  override def postStop(): Unit = {
    super.postStop()
    eventSub foreach { sub => sub.unsubscribe() }
  }
}
