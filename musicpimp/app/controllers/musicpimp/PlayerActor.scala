package controllers.musicpimp

import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{AuthedRequest, FullUrls}
import com.malliina.play.ws.{ActorConfig, JsonActor}
import play.api.libs.json.JsValue

import scala.concurrent.duration.DurationInt

class PlayerActor(player: ServerPlayer,
                  messageHandler: JsonHandlerBase,
                  conf: ActorConfig[AuthedRequest])(implicit mat: Materializer)
    extends JsonActor(conf) {
  // Keepalive for very old (Android) clients
  val pings = Source.tick(1.seconds, 5.seconds, 0)
  // Playback updates
  val ticks = Source.tick(200.millis, 900.millis, 0)
  val messageWriter = ServerMessage.jsonWriter(TrackJson.format(FullUrls.hostOnly(rh)))
  val apiVersion = PimpRequest.apiVersion(rh)
  implicit val w = TrackJson.writer(rh)
  val user = conf.user.user
  var allEventsSub: Option[UniqueKillSwitch] = None
  var timeSub: Option[Cancellable] = None
  var pingSub: Option[Cancellable] = None
  var previousPos = -1L
  val remoteInfo = RemoteInfo(user, apiVersion, FullUrls.hostOnly(rh), Target(json => out ! json))

  override def preStart(): Unit = {
    super.preStart()
    sendOut(WelcomeMessage)
    val killSwitch = player.allEvents
      .viaMat(KillSwitches.single)(Keep.right)
      .to(Sink.foreach { e =>
        out ! messageWriter.writes(e)
      })
      .run()
    allEventsSub = Option(killSwitch)
    timeSub = Option(
      ticks
        .to(Sink.foreach { _ =>
          onTick()
        })
        .run())
    pingSub = Option(
      pings
        .to(Sink.foreach { _ =>
          onPing()
        })
        .run())
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
    allEventsSub.foreach(_.shutdown())
    timeSub.foreach(_.cancel())
    pingSub.foreach(_.cancel())
  }
}
