package controllers

import com.mle.musicpimp.models.ClientInfo
import com.mle.play.Authenticator
import com.mle.play.controllers.AuthResult
import com.mle.play.ws.{JsonWebSockets, TrieClientStorage}
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

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}
