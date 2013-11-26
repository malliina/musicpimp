package controllers

import play.api.libs.json.JsValue
import models.ClientInfo
import play.api.mvc.{Call, Request, Controller, RequestHeader}
import play.api.libs.iteratee.Concurrent.Channel
import com.mle.play.ws.WebSocketController
import play.api.mvc.WebSocket.FrameFormatter

/**
 * A websockets controller. Subclasses shall implement onConnect, onMessage and onDisconnect.
 *
 * The WebSockets protocol doesn't handle authentication and authorization, so that's taken care of in the subscribe method.
 *
 * @author mle
 */
trait JsonWebSocketController extends WebSocketController with Controller with Secured {
  type Message = JsValue
  type Client = ClientInfo[Message]

  /**
   * Opens an authenticated WebSocket connection.
   *
   * This is the controller for requests to ws://... or wss://... URIs.
   *
   * @return a websocket connection using messages of type Message
   * @throws com.mle.musicpimp.exception.PimpException if authentication fails
   */
  def subscribe = ws(FrameFormatter.jsonFrame)

  def subscribeCall: Call

  def wsUrl(implicit request: RequestHeader) =
    subscribeCall.webSocketURL(secure = RequestHelpers.isHttps(request))

  //  def isIEMobile(request: RequestHeader) =
  //    request.headers.get(USER_AGENT).map(_.contains("IEMobile"))

  override def newClient(user: String, channel: Channel[Message])(implicit request: RequestHeader) =
    ClientInfo(channel, request, user)
}
