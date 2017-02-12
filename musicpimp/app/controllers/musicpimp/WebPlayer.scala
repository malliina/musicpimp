package controllers.musicpimp

import akka.stream.QueueOfferResult
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.json.JsonFormatVersions
import com.malliina.musicpimp.models.{ClientInfo, PimpUrl, RemoteInfo}
import com.malliina.play.models.Username
import controllers.musicpimp.WebPlayer.log
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Call

import scala.collection.mutable
import scala.concurrent.Future

class WebPlayer(security: SecureBase) extends PlayerSockets(security) {

  override val messageHandler: JsonHandlerBase = new WebPlayerMessageHandler {
    override def player(request: RemoteInfo): PimpWebPlayer = WebPlayer.this.player(request)
  }

  val players = mutable.Map.empty[Username, PimpWebPlayer]

  def player(request: RemoteInfo): PimpWebPlayer =
    players.getOrElseUpdate(request.user, new PimpWebPlayer(request, this))

  def add(request: RemoteInfo, track: TrackMeta) {
    val p = player(request)
    p.playlist add track
  }

  override def onConnectSync(client: ClientInfo[JsValue]): Unit = {
    super.onConnectSync(client)
    log info s"Connected ${client.describe}"
  }

  override def onDisconnectSync(client: ClientInfo[JsValue]): Unit = {
    super.onDisconnectSync(client)
    log info s"Disconnected ${client.describe}"
  }

  def remove(user: Username, trackIndex: Int): Unit =
    players.get(user).foreach(_.playlist delete trackIndex)

  def status(client: Client): JsValue = {
    val req = client.request
    val p = player(RemoteInfo(client.user, PimpUrl.hostOnly(req)))
    PimpRequest.apiVersion(req) match {
      case JsonFormatVersions.JSONv17 => p.statusEvent17
      case _ => p.statusEvent
    }
  }

  def openSocketCall: Call = routes.WebPlayer.openSocket()

  def unicast(user: Username, json: JsValue): Future[Seq[QueueOfferResult]] =
    Future.traverse(clientsSync.filter(_.user == user)) { c =>
      log.info(s"Offering $json to ${c.user}")
      c.channel offer json
    }
}

object WebPlayer {
  private val log = Logger(getClass)
}
