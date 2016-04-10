package controllers

import akka.stream.{Materializer, QueueOfferResult}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.json.JsonFormatVersions
import com.malliina.play.Authenticator
import play.api.libs.json.JsValue
import play.api.mvc.Call

import scala.collection.mutable
import scala.concurrent.Future

class WebPlayer(auth: Authenticator, mat: Materializer)
  extends PlayerSockets(auth, mat) {
  implicit val ec = mat.executionContext

  override val messageHandler: JsonHandlerBase = new WebPlayerMessageHandler {
    override def player(user: String): PimpWebPlayer = WebPlayer.this.player(user)
  }

  val players = mutable.Map.empty[String, PimpWebPlayer]

  def player(user: String): PimpWebPlayer =
    players.getOrElseUpdate(user, new PimpWebPlayer(user, this))

  def add(user: String, track: TrackMeta) {
    val p = player(user)
    p.playlist add track
  }

  def remove(user: String, trackIndex: Int): Unit =
    players.get(user).foreach(_.playlist.delete(trackIndex))

  def status(client: Client): JsValue = {
    val p = player(client.user)
    PimpRequest.apiVersion(client.request) match {
      case JsonFormatVersions.JSONv17 => p.statusEvent17
      case _ => p.statusEvent
    }
  }

  def openSocketCall: Call = routes.WebPlayer.openSocket()

  def unicast(user: String, json: JsValue): Future[Seq[QueueOfferResult]] =
    Future.traverse(clients.filter(_.user == user))(_.channel offer json)
}
