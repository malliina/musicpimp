package controllers.musicpimp

import akka.stream.QueueOfferResult
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages}
import com.malliina.musicpimp.models.PimpUrl
import controllers.musicpimp.ServerWS.log
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.Call
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Emits playback events to and accepts commands from listening clients.
  */
class ServerWS(val clouds: Clouds,
               security: SecureBase,
               handler: PlaybackMessageHandler)
  extends PlayerSockets(security) {

  val subscription = MusicPlayer.allEvents.subscribe(event => customBroadcast(event))
  override val messageHandler: JsonHandlerBase = handler
  val ticks = Observable.interval(900.millis)
  var poller: Option[Subscription] = None
  var previousPos = -1L

  def customBroadcast(message: ServerMessage) = {
    Future.traverse(clientsSync) { client =>
      val writer = ServerMessage.writer(PimpUrl.hostOnly(client.request))
      client.send(writer.writes(message))
    }
    val cloudHost = clouds.cloudHost
    val cloudJson = ServerMessage.writer(cloudHost).writes(message)
    clouds.sendIfConnected(cloudJson)
  }

  override def status(client: Client) = {
    implicit val w = TrackJson.writer(client.request)
    apiVersion(client) match {
      case JsonFormatVersions.JSONv17 => toJson(MusicPlayer.status17)
      case _ => toJson(MusicPlayer.status)
    }
  }

  def onTick() {
    val pos = MusicPlayer.position
    //    log info s"Broadcasting: $pos"
    val posSeconds = pos.toSeconds
    if (posSeconds != previousPos) {
      broadcast(JsonMessages.timeUpdated(pos))
      previousPos = posSeconds
    }
  }

  override def onConnectSync(client: Client): Unit = {
    super.onConnectSync(client)
    if (clientsSync.size == 1) {
      // first connection, start polling
      poller = Some(ticks.subscribe(_ => onTick()))
    }
  }

  override def onDisconnectSync(client: Client): Unit = {
    super.onDisconnectSync(client)
    if (clientsSync.isEmpty) {
      // stop polling
      poller.foreach(_.unsubscribe())
      poller = None
    }
  }

  def apiVersion(client: Client): String = PimpRequest.apiVersion(client.request)

  def openSocketCall: Call = routes.ServerWS.openSocket()

  override def broadcast(message: Message): Future[Seq[QueueOfferResult]] = {
    log debug s"$message to ${clientsSync.map(_.describe).mkString(", ")}"
    // sends the message to directly connected clients
    val ret = super.broadcast(message)
    // sends the message to the cloud, if this server is connected
    clouds sendIfConnected message
    ret
  }
}

object ServerWS {
  private val log = Logger(getClass)
}
