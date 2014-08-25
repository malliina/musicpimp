package controllers

import com.mle.play.ws.WebSocketBase
import com.mle.util.Log
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.mvc.{RequestHeader, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 * @author mle
 */
trait WebSocketController2 extends WebSocketBase with Log {
  /**
   * Opens a WebSocket connection.
   *
   * This is the controller for requests to ws://... or wss://... URIs.
   *
   * The implementation is problematic because a websocket connection
   * appears to be opened even if authentication fails, if only for a
   * very brief moment. I would prefer to return some erroneous HTTP
   * status code when authentication fails but I don't know how to.
   *
   * @return a websocket connection using messages of type Message
   */
  def ws(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    ws2(welcomeMessage.fold(Enumerator.empty[Message])(Enumerator[Message](_)))

  def ws2(initialEnumerator: Enumerator[Message])(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.using[Message](request => {
      authenticate(request).fold({
        // authentication failed
        log warn s"Unauthorized WebSocket connection attempt from: ${request.remoteAddress}"
        val in = Iteratee.foreach[Message](_ => ())
        val out = Enumerator.eof[Message]
        (in, out)
      })(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user, channel)(request)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo))
          .map(_ => onDisconnect(clientInfo))
        val enumerator = Enumerator.interleave(initialEnumerator, out)
        onConnect(clientInfo)
        (in, enumerator)
      })
    })

  def welcomeMessage: Option[Message] = None

  def authenticate(implicit request: RequestHeader): Option[String]
}
