package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.models.{ClientInfo, User}
import com.malliina.play.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{JsonWebSockets, TrieClientStorage}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

abstract class PimpSockets(auth: Authenticator, val mat: Materializer)
  extends JsonWebSockets
    with TrieClientStorage {

  type Client = ClientInfo[Message]
  type AuthSuccess = AuthedRequest

  // TODO fix
  val security = new SecureBase(auth, mat)

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] = {
    security.authenticate(req).flatMap(opt => opt
      .map(Future.successful)
      .getOrElse(Future.failed(new NoSuchElementException(s"Auth failed from ${req.remoteAddress}"))))
  }

  override def newClient(user: AuthSuccess, channel: SourceQueue[JsValue], request: RequestHeader): ClientInfo[JsValue] =
    ClientInfo(channel, request, User(user.user.name))

  override def welcomeMessage(client: Client): Option[Message] =
    Some(com.malliina.play.json.JsonMessages.welcome)
}
