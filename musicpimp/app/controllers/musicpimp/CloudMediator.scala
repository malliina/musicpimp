package controllers.musicpimp

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import org.apache.pekko.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.{CloudCommand, Connect, Disconnect, Noop}
import com.malliina.play.ws.Mediator.Broadcast
import com.malliina.play.ws.ReplayMediator
import controllers.musicpimp.CloudMediator.log
import io.circe.Json
import io.circe.syntax.EncoderOps
import play.api.Logger
import play.api.mvc.RequestHeader

object CloudMediator:
  private val log = Logger(getClass)

class CloudMediator(clouds: Clouds) extends ReplayMediator(1):
  private val jsonEvents = clouds.connection.map(event => event.asJson)
  private var subscription: Option[UniqueKillSwitch] = None
  implicit val as: ActorSystem = context.system

  override def preStart(): Unit =
    super.preStart()
    val killSwitch = jsonEvents
      .viaMat(KillSwitches.single)(Keep.right)
      .to(Sink.foreach: json =>
        log.info(s"Broadcast $json")
        self ! Broadcast(json)
      )
      .run()
    subscription = Option(killSwitch)
    clouds.emitLatest()

  override def onClientMessage(message: Json, rh: RequestHeader): Unit =
    message
      .as[CloudCommand]
      .map(handleCommand)
      .left
      .map(err => log.error(s"Invalid JSON '$message'. $err"))

  def handleCommand(cmd: CloudCommand): Any =
    cmd match
      case Connect(id) =>
        clouds.connect(Option(id).filter(_.id.nonEmpty))
      case Disconnect =>
        clouds.disconnectAndForgetAsync()
      case Noop =>
        ()

  override def postStop(): Unit =
    super.postStop()
    subscription foreach { sub =>
      sub.shutdown()
    }
