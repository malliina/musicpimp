package controllers.pimpcloud

import java.util.UUID

import akka.stream.Materializer
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.ws.StreamData
import com.malliina.play.auth.Auth
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.{Password, Username}
import com.malliina.ws.{RxStmStorage, ServerSocket}
import controllers.pimpcloud.Servers.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.concurrent.Future

/** WebSocket for connected MusicPimp servers.
  *
  * Pushes player events sent by servers to any connected phones, and responds to requests.
  */
abstract class Servers(mat: Materializer)
  extends ServerSocket(RxStmStorage[PimpServerSocket](), mat) {
  // not a secret but avoids unintentional connections
  val serverPassword = Password("pimp")

  implicit val writer = Writes[PimpServerSocket](o => Json.obj(
    Id -> o.id,
    Address -> o.headers.remoteAddress
  ))
  val usersJson = storage.users.map(list => Json.obj(Event -> ServersKey, Body -> list))

  val streamSubject = BehaviorSubject[Seq[StreamData]](Nil)
  val uuidsJson: Observable[JsValue] = streamSubject.map(streams => Json.obj(
    Event -> JsonStrings.RequestsKey,
    Body -> streams
  ))

  def updateRequestList() = ongoingTransfers.foreach(ts => streamSubject.onNext(ts.toSeq))

  private def ongoingTransfers = connectedServers.map(_.flatMap(_.fileTransfers.snapshot))

  override def openSocketCall: Call = routes.Servers.openSocket()

  def newID(): Username = Username(UUID.randomUUID().toString take 5)

  /** The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
    * new clients, or a previously used cloud ID for old clients. The password must be pimp.
    *
    * @param request
    * @return a valid cloud ID, or None if the cloud ID generation failed
    */
  override def authenticateAsync(request: RequestHeader): Future[AuthedRequest] = {
    Auth.basicCredentials(request)
      .filter(_.password == serverPassword)
      .map { creds =>
        val user = creds.username
        val cloudID: Future[Username] =
          if (user.name.nonEmpty) {
            isConnected(user) flatMap { connected =>
              if (connected) {
                val msg = s"Unable to register client: $user. Another client with that ID is already connected."
                log warn msg
                Future.failed(new NoSuchElementException(msg))
              } else {
                Future.successful(user)
              }
            }
          } else {
            val id = newID()
            isConnected(id) flatMap { connected =>
              if (connected) {
                val msg = s"A collision occurred while generating a random client ID: $id. Unable to register client."
                log error msg
                Future.failed(new NoSuchElementException(msg))
              } else {
                Future.successful(id)
              }
            }
          }
        cloudID map (id => AuthedRequest(id, request))
      }.getOrElse {
      Future.failed(new NoSuchElementException)
    }
  }


  override def welcomeMessage(client: PimpServerSocket): Option[JsValue] = {
    Some(Json.obj(Cmd -> RegisteredKey, Body -> Json.obj(Id -> client.id)))
  }

  def isConnected(serverID: Username): Future[Boolean] =
    connectedServers.exists(cs => cs.exists(_.id.id == serverID.name))

  def connectedServers: Future[Set[PimpServerSocket]] = Future.successful(storage.clients.toSet)

  override def onMessage(msg: JsValue, client: PimpServerSocket): Boolean = {
    log debug s"Got message: $msg from client: $client"

    val isUnregister = false // (msg \ CMD).validate[String].filter(_ == UNREGISTER).isSuccess
    if (isUnregister) {
      //      identities remove client.id
      false
    } else {
      val clientHandledMessage = client complete msg
      // forwards non-requested events to any connected phones

      // The fact a client refuses to handle a response doesn't mean it's meant for someone else. The response may for
      // example have been ignored by the client because it arrived too late. This logic is thus not solid. The
      // consequence is that clients may receive unsolicited messages occasionally. But they should ignore those anyway,
      // so we accept this failure.
      if (!clientHandledMessage) {
        sendToPhone(msg, client)
      }
      clientHandledMessage
    }
  }

  def sendToPhone(msg: JsValue, client: PimpServerSocket): Unit
}

object Servers {
  private val log = Logger(getClass)
}

case class ServerRequest(request: UUID, socket: PimpServerSocket)

case class PhoneConnection(user: Username, server: PimpServerSocket)
