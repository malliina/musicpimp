package controllers

import com.malliina.musicpimp.models.ClientInfo
import com.malliina.play.Authenticator
import com.malliina.play.controllers.AuthResult
import com.malliina.play.ws.{JsonWebSockets, TrieClientStorage}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc.{Controller, RequestHeader}

import scala.concurrent.Future

/**
 * @author Michael
 */
abstract class PimpSockets(auth: Authenticator) extends Controller with JsonWebSockets with TrieClientStorage {
  type Client = ClientInfo[Message]
  type AuthSuccess = AuthResult

  val security = new SecureBase(auth)

  override def authenticateAsync(req: RequestHeader): Future[AuthResult] = {
    security.authenticate(req).flatMap(opt => opt
      .map(Future.successful)
      .getOrElse(Future.failed(new NoSuchElementException(s"Auth failed"))))
  }

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader) =
    ClientInfo(channel, request, user.user)

  override def welcomeMessage(client: Client): Option[Message] = Some(com.malliina.play.json.JsonMessages.welcome)
}
