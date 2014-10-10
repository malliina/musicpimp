package controllers

import com.mle.musicpimp.audio._
import com.mle.musicpimp.cloud.Clouds
import com.mle.musicpimp.json.{JsonFormatVersions, JsonMessages}
import com.mle.play.ws.TrieClientStorage
import com.mle.util.Log
import play.api.libs.json.Json.toJson
import play.api.mvc.Call
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationLong

/**
 * Emits playback events to and accepts commands from listening clients.
 *
 * @author mle
 */
trait ServerWS extends MyJsonWebSocketController with Log {
  override val messageHandler: JsonHandlerBase = PlaybackMessageHandler

  override def status(client: Client) = apiVersion(client) match {
    case JsonFormatVersions.JSONv17 => toJson(MusicPlayer.status17)
    case _ => toJson(MusicPlayer.status)
  }

  val ticks = Observable.interval(900.millis)
  var poller: Option[Subscription] = None
  var previousPos = -1L

  def onTick() {
    val pos = MusicPlayer.position
    //    log info s"Broadcasting: $pos"
    val posSeconds = pos.toSeconds
    if (posSeconds != previousPos) {
      broadcast(JsonMessages.timeUpdated(pos))
      previousPos = posSeconds
    }
  }

  override def onConnect(client: Client): Unit = {
    super.onConnect(client)
    if (clients.size == 1) {
      // first connection, start polling
      poller = Some(ticks.subscribe(_ => onTick()))
    }
  }


  override def onDisconnect(client: Client): Unit = {
    super.onDisconnect(client)
    if (clients.isEmpty) {
      // stop polling
      poller.foreach(_.unsubscribe())
      poller = None
    }
  }

  def apiVersion(client: Client): String = PimpRequest.apiVersion(client.request)

  def openSocketCall: Call = routes.ServerWS.openSocket()

  override def broadcast(message: Message): Unit = {
    super.broadcast(message)
    Clouds sendIfConnected message
  }
}

object ServerWS extends ServerWS