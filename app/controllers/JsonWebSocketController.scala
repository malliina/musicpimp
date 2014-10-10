package controllers

import com.mle.play.ws.JsonWebSockets
import models.ClientInfo
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc.{Controller, RequestHeader}

/**
 * A websockets controller. Subclasses shall implement onConnect, onMessage and onDisconnect.
 *
 * The WebSockets protocol doesn't handle authentication and authorization, so that's taken care of in the subscribe
 * method.
 *
 * @author mle
 */
trait JsonWebSocketController extends JsonWebSockets with Controller with Secured {
  type Client = ClientInfo[Message]
  type AuthSuccess = String

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader) =
    ClientInfo(channel, request, user)
}
