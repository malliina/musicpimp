package controllers.musicpimp

import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.models.ClientInfo
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{JsonWebSockets, TrieClientStorage}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

abstract class PimpSockets(security: SecureBase)
  extends JsonWebSockets(security.mat)
    with TrieClientStorage {

  type Client = ClientInfo[Message]
  type AuthSuccess = AuthedRequest

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] = {
    security.authenticate(req).flatMap(opt => opt
      .map(fut)
      .getOrElse(Future.failed(new NoSuchElementException(s"Auth failed from ${req.remoteAddress}"))))
  }

  override def newClient(user: AuthSuccess,
                         channel: SourceQueue[JsValue],
                         request: RequestHeader): ClientInfo[JsValue] =
    ClientInfo(channel, request, user.user)

  override def welcomeMessage(client: Client): Option[Message] =
    Some(com.malliina.play.json.JsonMessages.welcome)
}