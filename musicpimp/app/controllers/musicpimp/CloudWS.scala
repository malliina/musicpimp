package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models._
import com.malliina.play.ActorExecution
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.{MediatorSockets, ReplayMediator}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import rx.lang.scala.Subscription

object CloudWS {
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"

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

  private val jsonEvents = clouds.connection.map(event => Json.toJson(event))
  private var subscription: Option[Subscription] = None

  override def preStart(): Unit = {
    super.preStart()
    log info "Starting mediator"
    val sub = jsonEvents.subscribe(
      e => {
        log.info(s"Broadcast $e")
        self ! Broadcast(e)
      },
      (err: Throwable) => log.error("WebSocket error.", err),
      () => ())
    subscription = Option(sub)
  }

  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit = {
    message.validate[CloudCommand]
      .map(handleCommand)
      .recoverTotal(err => log.error(s"Invalid JSON '$message'. $err"))
  }

  def handleCommand(cmd: CloudCommand): Any = {
    cmd match {
      case Connect(id) =>
        clouds.connect(Option(id).filter(_.id.nonEmpty))
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
