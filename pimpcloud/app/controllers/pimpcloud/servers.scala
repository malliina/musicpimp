package controllers.pimpcloud

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.stream.Materializer
import akka.util.Timeout
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models.{PimpServer, PimpServers, PimpStreams}
import com.malliina.pimpcloud.ws.StreamData
import com.malliina.play.ActorExecution
import com.malliina.play.auth._
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.{Password, Username}
import com.malliina.play.ws._
import controllers.pimpcloud.ServerMediator._
import controllers.pimpcloud.Servers.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** WebSocket for connected MusicPimp servers.
  *
  * Pushes player events sent by servers to any connected phones, and responds to requests.
  */
class Servers(phoneMediator: ActorRef, val ctx: ActorExecution) {
  implicit val ec = ctx.executionContext
  // not a secret but avoids unintentional connections
  val serverPassword = Password("pimp")

  /** The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
    * new clients, or a previously used cloud ID for old clients. The password must be `serverPassword`.
    *
    * @param rh headers
    * @return auth failure or success
    */
  def authAsync(rh: RequestHeader): Future[Either[AuthFailure, AuthedRequest]] =
    Auth.basicCredentials(rh) map { creds =>
      if (creds.password == serverPassword) {
        val user = creds.username
        val cloudId = if (user.name.nonEmpty) CloudID(user.name) else newID()
        isConnected(cloudId) map { connected =>
          if (connected) {
            log warn s"Unable to register client: '$user'. Another client with that ID is already connected."
            Left(InvalidCredentials(rh))
          } else {
            Right(AuthedRequest(user, rh))
          }
        }
      } else {
        fut(Left(InvalidCredentials(rh)))
      }
    } getOrElse {
      fut(Left(MissingCredentials(rh)))
    }

  def newID(): CloudID = CloudID(UUID.randomUUID().toString take 5)

  def fut[T](t: T): Future[T] = Future.successful(t)

  val serverAuth = Authenticator(authAsync)
  val serverMediator = ctx.actorSystem.actorOf(Props(new ServerMediator))
  val serverSockets = new Sockets(serverAuth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new ServerActor(serverMediator, phoneMediator, conf, ctx.materializer))
  }

  import akka.pattern.ask

  implicit val timeout = Timeout(5.seconds)

  def isConnected(serverID: CloudID): Future[Boolean] =
    (serverMediator ? Exists(serverID)).mapTo[Boolean]

  def connectedServers: Future[Set[PimpServerSocket]] = {
    (serverMediator ? GetServers).mapTo[Set[PimpServerSocket]]
  }

}

class ServerActor(serverMediator: ActorRef,
                  phoneMediator: ActorRef,
                  conf: ActorConfig[AuthedRequest],
                  mat: Materializer)
  extends JsonActor(conf) {
  val cloudId = CloudID(conf.user.user.name)
  val server = new PimpServerSocket(out, cloudId, conf.rh, mat, () => serverMediator ! StreamsUpdated)

  override def preStart() = {
    out ! Json.obj(Cmd -> ServerActor.RegisteredKey, Body -> Json.obj(Id -> server.id))
    serverMediator ! ServerJoined(server, out)
  }

  override def onMessage(message: JsValue) = {
    val completed = server complete message
    if (!completed) {
      // sendToPhone, i.e. send to phones connected to this server
      phoneMediator ! ServerEvent(message, server.id)
    }
  }
}

object ServerActor {
  val RegisteredKey = "registered"
}

class ServerMediator extends Actor with ActorLogging {
  var servers: Set[PimpServerSocket] = Set.empty
  var listeners: Set[ActorRef] = Set.empty

  def serversJson: PimpServers = {
    val ss = servers map { server =>
      PimpServer(server.id, server.headers.remoteAddress)
    }
    PimpServers(ss.toSeq)
  }

  def ongoing = servers.flatMap(_.fileTransfers.snapshot)

  def ongoingJson(streams: Set[StreamData]): PimpStreams =
    PimpStreams(streams.toSeq)

  def receive: Receive = {
    case Listen(listener) =>
      context watch listener
      listeners += listener
      listener ! Json.toJson(ongoingJson(ongoing))
      listener ! Json.toJson(serversJson)
    case ServerJoined(server, out) =>
      context watch out
      servers += server
      sendUpdate(serversJson)
    case Exists(id) =>
      val exists = servers.exists(_.id == id)
      sender() ! exists
    case GetServers =>
      sender() ! servers
    case StreamsUpdated =>
      sendUpdate(ongoingJson(ongoing))
    case Terminated(out) =>
      servers.find(_.jsonOut == out) foreach { server =>
        servers -= server
        sendUpdate(serversJson)
        // TODO kill all phones connected to this server
      }
      listeners.find(_ == out) foreach { listener =>
        listeners -= listener
      }
  }

  def sendUpdate[C: Writes](message: C) = {
    val json = Json.toJson(message)
    listeners foreach { listener =>
      listener ! json
    }
  }
}

object ServerMediator {

  case class ServerJoined(server: PimpServerSocket, out: ActorRef)

  case class ServerEvent(message: JsValue, from: CloudID)

  case class Exists(id: CloudID)

  case object GetServers

  case object StreamsUpdated

  case class Listen(listener: ActorRef)

}

object Servers {
  private val log = Logger(getClass)
}

case class ServerRequest(request: UUID, socket: PimpServerSocket)

case class PhoneConnection(user: Username, server: PimpServerSocket)
