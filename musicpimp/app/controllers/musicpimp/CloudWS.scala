package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio.JsonCmd
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.musicpimp.models.CloudID
import com.malliina.play.ActorContext
import com.malliina.play.auth.{Authenticator, InvalidCredentials}
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.{Mediator, ReplayMediator, SimpleSockets}
import controllers.musicpimp.CloudCommand.{Connect, Disconnect, Noop}
import controllers.musicpimp.CloudEvent.Disconnecting
import controllers.musicpimp.CloudWS.{ConnectCmd, DisconnectCmd, Id}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Security}
import rx.lang.scala.Subscription

sealed abstract class CloudEvent(event: String)

object CloudEvent {
  val IdKey = "id"
  val ConnectedKey = "connected"
  val ConnectingKey = "connecting"
  val DisconnectedKey = "disconnected"
  val DisconnectingKey = "disconnecting"

  implicit val writer = Writes[CloudEvent] {
    case Connected(id) => JsonMessages.event(ConnectedKey, IdKey -> id)
    case Disconnected(reason) => JsonMessages.event(DisconnectedKey, JsonStrings.Reason -> reason)
    case Connecting => JsonMessages.event(ConnectingKey)
    case Disconnecting => JsonMessages.event(DisconnectingKey)
  }

  case class Connected(id: CloudID) extends CloudEvent(ConnectedKey)

  case class Disconnected(reason: String) extends CloudEvent(DisconnectedKey)

  case object Connecting extends CloudEvent(ConnectingKey)

  case object Disconnecting extends CloudEvent(DisconnectingKey)

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
  val Subscribe = "subscribe"
}

class CloudWS(clouds: Clouds, ctx: ActorContext) {
  val auth = Authenticator[AuthedRequest] { rh =>
    val result = rh.session.get(Security.username).map(Username.apply)
      .map(user => Right(new AuthedRequest(user, rh)))
      .getOrElse(Left(InvalidCredentials(rh)))
    fut(result)
  }

  val sockets = new SimpleSockets[AuthedRequest](Props(new CloudMediator(clouds)), auth, ctx)

  def newSocket = sockets.newSocket
}

class CloudMediator(clouds: Clouds) extends ReplayMediator(1) {
  val jsonEvents = clouds.connection.map(event => Json.toJson(event))
  var subscription: Option[Subscription] = None

  override def preStart(): Unit = {
    val sub = jsonEvents.subscribe(
      e => self ! Broadcast(e),
      (err: Throwable) => log.error(s"WebSocket error.", err),
      () => ())
    subscription = Option(sub)
  }

  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit = {
    val cmd = new JsonCmd(message)
    val parsed: JsResult[CloudCommand] = cmd.command flatMap {
      case DisconnectCmd => JsSuccess(Disconnect)
      case ConnectCmd => cmd.key[CloudID](Id).map(id => Connect(id))
      case CloudWS.Subscribe => JsSuccess(Noop)
      case other => JsError(s"Unknown command: '$other'.")
    }
    parsed
      .map(handleCommand)
      .recoverTotal(err => log.error(s"Invalid JSON: '$message'., $err"))
  }

  def handleCommand(cmd: CloudCommand): Any = {
    cmd match {
      case Connect(id) =>
        clouds.connect(Option(id))
      case Disconnect =>
        clouds.disconnectAndForgetAsync()
      case Noop =>
        ()
    }
  }

  override def postStop(): Unit = {
    subscription foreach { sub => sub.unsubscribe() }
  }
}
