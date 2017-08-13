package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio.JsonCmd
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.CloudID
import com.malliina.play.ActorExecution
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.{MediatorSockets, ReplayMediator}
import controllers.musicpimp.CloudCommand.{Connect, Disconnect, Noop}
import controllers.musicpimp.CloudWS.{ConnectCmd, DisconnectCmd, Id, SubscribeCmd}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import rx.lang.scala.Subscription

sealed trait CloudCommand

object CloudCommand {

  case class Connect(id: CloudID) extends CloudCommand

  case object Disconnect extends CloudCommand

  case object Noop extends CloudCommand

}

object CloudWS {
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"
  val SubscribeCmd = "subscribe"

  val sessionAuth = Auths.session
}

class CloudWS(clouds: Clouds, ctx: ActorExecution) {
  val sockets = new MediatorSockets[AuthedRequest](Props(new CloudMediator(clouds)), CloudWS.sessionAuth, ctx)

  def openSocket = sockets.newSocket
}

object CloudMediator {
  private val log = Logger(getClass)
}

class CloudMediator(clouds: Clouds) extends ReplayMediator(1) {
  import CloudMediator.log
  val jsonEvents = clouds.connection.map(event => Json.toJson(event))
  var subscription: Option[Subscription] = None

  override def preStart(): Unit = {
    super.preStart()
    val sub = jsonEvents.subscribe(
      e => self ! Broadcast(e),
      (err: Throwable) => log.error("WebSocket error.", err),
      () => ())
    subscription = Option(sub)
  }

  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit = {
    val cmd = new JsonCmd(message)
    val parsed: JsResult[CloudCommand] = cmd.command flatMap {
      case DisconnectCmd => JsSuccess(Disconnect)
      case ConnectCmd => cmd.key[CloudID](Id).map(id => Connect(id))
      case SubscribeCmd => JsSuccess(Noop)
      case other => JsError(s"Unknown command '$other'.")
    }
    parsed
      .map(handleCommand)
      .recoverTotal(err => log.error(s"Invalid JSON '$message'. $err"))
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
    super.postStop()
    subscription foreach { sub => sub.unsubscribe() }
  }
}
