package controllers

import akka.stream.{Materializer, QueueOfferResult}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.json.JsonFormatVersions
import com.malliina.musicpimp.models.{ClientInfo, PimpUrl, RemoteInfo}
import com.malliina.play.Authenticator
import com.malliina.play.http.RequestInfo
import controllers.WebPlayer.log
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Call, RequestHeader}

import scala.collection.mutable
import scala.concurrent.Future

class WebPlayer(auth: Authenticator, mat: Materializer)
  extends PlayerSockets(auth, mat) {
  implicit val ec = mat.executionContext

  override val messageHandler: JsonHandlerBase = new WebPlayerMessageHandler {
    override def player(request: RemoteInfo): PimpWebPlayer = WebPlayer.this.player(request)
  }

  val players = mutable.Map.empty[String, PimpWebPlayer]

  def player(request: RemoteInfo): PimpWebPlayer =
    players.getOrElseUpdate(request.user, new PimpWebPlayer(request, this))

  def add(request: RemoteInfo, track: TrackMeta) {
    val p = player(request)
    p.playlist add track
  }

  override def onConnect(client: ClientInfo[JsValue]): Unit = {
    super.onConnect(client)
    log info s"Connected ${client.describe}"
  }

  override def onDisconnect(client: ClientInfo[JsValue]): Unit = {
    super.onDisconnect(client)
    log info s"Disconnected ${client.describe}"
  }

  def remove(user: String, trackIndex: Int): Unit =
    players.get(user).foreach(_.playlist delete trackIndex)

  def status(client: Client): JsValue = {
    val p = player(RemoteInfo(client.user, PimpUrl.hostOnly(client.request)))
    PimpRequest.apiVersion(client.request) match {
      case JsonFormatVersions.JSONv17 => p.statusEvent17
      case _ => p.statusEvent
    }
  }

  def openSocketCall: Call = routes.WebPlayer.openSocket()

  def unicast(user: String, json: JsValue): Future[Seq[QueueOfferResult]] =
    Future.traverse(clients.filter(_.user == user))(_.channel offer json)
}

object WebPlayer {
  private val log = Logger(getClass)
}
