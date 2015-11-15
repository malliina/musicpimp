package controllers

import com.mle.musicpimp.audio._
import com.mle.musicpimp.json.JsonFormatVersions
import com.mle.play.Authenticator
import com.mle.util.Log
import play.api.libs.json.JsValue
import play.api.mvc.Call

import scala.collection.mutable

/**
 * @author mle
 */
class WebPlayer(auth: Authenticator) extends PlayerSockets(auth) with Log {
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

  def unicast(user: String, json: JsValue) = clients.filter(_.user == user).foreach(_.channel push json)
}
