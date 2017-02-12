package controllers.musicpimp

import com.malliina.maps.StmItemMap
import com.malliina.musicpimp.audio.JsonCmd
import com.malliina.musicpimp.cloud.{CloudID, Clouds}
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.play.controllers.Streaming
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import controllers.musicpimp.CloudCommand.{Connect, Disconnect, Noop}
import controllers.musicpimp.CloudWS.{ConnectCmd, DisconnectCmd, Id, log}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Security}
import rx.lang.scala.Subscription

import scala.concurrent.Future

sealed abstract class CloudEvent(event: String)

object CloudEvent {
  val IdKey = "id"
  val ConnectingStr = "connecting"
  val ConnectedStr = "connected"
  val DisconnectedStr = "disconnected"

  implicit val writer = Writes[CloudEvent] {
    case Connected(id) => JsonMessages.event(ConnectedStr, IdKey -> id)
    case Disconnected(reason) => JsonMessages.event(DisconnectedStr, JsonStrings.Reason -> reason)
    case Connecting => JsonMessages.event(ConnectingStr)
  }

  case class Connected(id: CloudID) extends CloudEvent(ConnectedStr)

  case class Disconnected(reason: String) extends CloudEvent(DisconnectedStr)

  case object Connecting extends CloudEvent(ConnectingStr)

}

sealed trait CloudCommand

object CloudCommand {

  case class Connect(id: CloudID) extends CloudCommand

  case object Disconnect extends CloudCommand

  case object Noop extends CloudCommand

}

object CloudWS {
  private val log = Logger(getClass)

  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"
  val tmp: Option[String] = None
}

class CloudWS(clouds: Clouds, security: SecureBase) extends Streaming(security.mat) {
  val jsonEvents = clouds.connection.map(event => Json.toJson(event))

  override def openSocketCall = routes.CloudWS.openSocket()

  override lazy val subscriptions = StmItemMap.empty[Client, Subscription]

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] =
    req.session.get(Security.username).map(Username.apply)
      .map(user => fut(new AuthedRequest(user, req)))
      .getOrElse(Future.failed(new NoSuchElementException))

  override def onMessage(msg: JsValue, client: Client): Boolean = {
    super.onMessage(msg, client)
    val cmd = new JsonCmd(msg)
    val parsed: JsResult[CloudCommand] = cmd.command flatMap {
      case DisconnectCmd => JsSuccess(Disconnect)
      case ConnectCmd => cmd.key[CloudID](Id).map(id => Connect(id))
      case SUBSCRIBE => JsSuccess(Noop)
      case other => JsError(s"Unknown command: $other")
    }
    parsed
      .map(handleCommand)
      .recoverTotal(err => log.error(s"Invalid JSON: '$msg', $err"))
    parsed.isSuccess
  }

  def handleCommand(cmd: CloudCommand): Any = {
    cmd match {
      case Connect(id) => clouds.connect(Option(id))
      case Disconnect => clouds.disconnectAndForget()
      case Noop => ()
    }
  }
}
