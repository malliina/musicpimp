package controllers

import com.mle.play.controllers.AuthResult
import com.mle.play.ws.{SyncAuth, JsonWebSockets, TrieClientStorage}
import models.ClientInfo
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc.RequestHeader

/**
 * @author Michael
 */
trait PimpSocket extends JsonWebSockets with TrieClientStorage with Secured with SyncAuth {
  type Client = ClientInfo[Message]
  type AuthSuccess = AuthResult

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader) =
    ClientInfo(channel, request, user.user)

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}
