package controllers.musicpimp

import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.{CloudCommand, Connect, Disconnect, Noop}
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.ReplayMediator
import controllers.musicpimp.CloudMediator.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import rx.lang.scala.Subscription

object CloudMediator {
  private val log = Logger(getClass)
}

class CloudMediator(clouds: Clouds) extends ReplayMediator(1) {
  private val jsonEvents = clouds.connection.map(event => Json.toJson(event))
  private var subscription: Option[Subscription] = None

  override def preStart(): Unit = {
    super.preStart()
    val sub = jsonEvents.subscribe(
      e => {
        log.info(s"Broadcast $e")
        self ! Broadcast(e)
      },
      (err: Throwable) => log.error("WebSocket error.", err),
      () => ())
    subscription = Option(sub)
  }

  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit =
    message.validate[CloudCommand]
      .map(handleCommand)
      .recoverTotal(err => log.error(s"Invalid JSON '$message'. $err"))

  def handleCommand(cmd: CloudCommand): Any =
    cmd match {
      case Connect(id) =>
        clouds.connect(Option(id).filter(_.id.nonEmpty))
      case Disconnect =>
        clouds.disconnectAndForgetAsync()
      case Noop =>
        ()
    }

  override def postStop(): Unit = {
    super.postStop()
    subscription foreach { sub => sub.unsubscribe() }
  }
}
