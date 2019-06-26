package controllers.musicpimp

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, KillSwitches, UniqueKillSwitch}
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.{CloudCommand, Connect, Disconnect, Noop}
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.ReplayMediator
import controllers.musicpimp.CloudMediator.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader

object CloudMediator {
  private val log = Logger(getClass)
}

class CloudMediator(clouds: Clouds) extends ReplayMediator(1) {
  private val jsonEvents = clouds.connection.map(event => Json.toJson(event))
  private var subscription: Option[UniqueKillSwitch] = None
  implicit val mat = ActorMaterializer()

  override def preStart(): Unit = {
    super.preStart()
    val killSwitch = jsonEvents
      .viaMat(KillSwitches.single)(Keep.right)
      .to(Sink.foreach { json =>
        log.info(s"Broadcast $json")
        self ! Broadcast(json)
      })
      .run()
    subscription = Option(killSwitch)
  }

  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit =
    message
      .validate[CloudCommand]
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
    subscription foreach { sub =>
      sub.shutdown()
    }
  }
}
