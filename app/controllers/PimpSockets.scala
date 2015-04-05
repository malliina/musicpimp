package controllers

import com.mle.models.ClientInfo
import com.mle.play.controllers.AuthResult
import com.mle.play.ws.{JsonWebSockets, SyncAuth, TrieClientStorage}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc.{Controller, RequestHeader}

/**
 * @author Michael
 */
trait PimpSockets extends Controller with JsonWebSockets with TrieClientStorage with Secured with SyncAuth {
  type Client = ClientInfo[Message]
  type AuthSuccess = AuthResult

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader) =
    ClientInfo(channel, request, user.user)

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}
