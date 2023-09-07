package controllers.pimpcloud

import java.util.UUID
import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import com.malliina.musicpimp.cloud.{Constants, PimpServerSocket}
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.ws.ServerMediator._
import com.malliina.pimpcloud.ws.{ServerActor, ServerMediator}
import com.malliina.play.ActorExecution
import com.malliina.play.auth._
import com.malliina.play.http.{AuthedRequest, Proxies}
import com.malliina.play.models.AuthInfo
import com.malliina.play.ws._
import com.malliina.values.Username
import controllers.pimpcloud.Servers.log
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.mvc._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt

object Servers {
  private val log = Logger(getClass)
}

/** WebSocket for connected MusicPimp servers.
  *
  * Pushes player events sent by servers to any connected phones, and responds to requests.
  */
class Servers(phoneMediator: ActorRef, val ctx: ActorExecution, errorHandler: HttpErrorHandler) {
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  /** The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
    * new clients, or a previously used cloud ID for old clients.
    *
    * @param rh headers
    * @return auth failure or success
    */
  def authAsync(rh: RequestHeader): Future[Either[AuthFailure, AuthedRequest]] =
    Auth.basicCredentials(rh) map { creds =>
      if (creds.password == Constants.pass) {
        val user = creds.username
        val cloudId = if (user.name.trim.nonEmpty) CloudID(user.name) else newID()
        isConnected(cloudId) map { connected =>
          if (connected) {
            log warn s"Unable to register client: '$cloudId'. Another client with that ID is already connected."
            Left(InvalidCredentials(rh))
          } else {
            Right(AuthedRequest(Username(cloudId.id), rh))
          }
        }
      } else {
        fut(Left(InvalidCredentials(rh)))
      }
    } getOrElse {
      log warn s"No credentials for request from '${Proxies.realAddress(rh)}'."
      fut(Left(MissingCredentials(rh)))
    }

  def newID(): CloudID = CloudID(UUID.randomUUID().toString take 5)

  def fut[T](t: T): Future[T] = Future.successful(t)

  val serverAuth = Authenticator(authAsync)
  val serverMediator = ctx.actorSystem.actorOf(Props(new ServerMediator))
  val serverSockets = new Sockets(serverAuth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(
        new ServerActor(
          serverMediator,
          phoneMediator,
          conf,
          errorHandler,
          ctx.materializer,
          ctx.actorSystem.scheduler
        )
      )
  }

  import akka.pattern.ask

  implicit val timeout: Timeout = Timeout(5.seconds)

  def isConnected(serverID: CloudID): Future[Boolean] =
    (serverMediator ? Exists(serverID)).mapTo[Boolean]

  def connectedServers: Future[Set[PimpServerSocket]] = {
    (serverMediator ? GetServers).mapTo[Set[PimpServerSocket]]
  }

}

case class ServerRequest(request: RequestID, socket: PimpServerSocket) extends AuthInfo {
  override def user: Username = Username(socket.id.id)
  override def rh: RequestHeader = socket.headers
}
