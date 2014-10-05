package com.mle.ws

import java.net.URI

import com.mle.security.SSLUtils
import com.mle.util.Log
import org.java_websocket.client.{DefaultSSLWebSocketClientFactory, WebSocketClient}
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.{JsValue, Json, Writes}
import rx.lang.scala.{Observable, Subject}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}


/**
 *
 * @author mle
 */
class JsonWebSocketClient(uri: String, username: String, password: String, additionalHeaders: (String, String)*)
  extends JsonWebSocket with Log {
  val ACCEPT = "Accept"
  val AUTHORIZATION = "Authorization"
  val JSON = "application/json"

  private val connectPromise = Promise[Unit]()

  private val headers = Map(
    AUTHORIZATION -> HttpUtil.authorizationValue(username, password)
  ) ++ additionalHeaders.toMap
  protected val subject = Subject[JsValue]()
  //  protected val eventsSubject = Subject[SocketEvent]()

  def messages: Observable[JsValue] = subject

  //  def events: Observable[SocketEvent] = status

  val client = new WebSocketClient(URI create uri, new Draft_10, headers, 0) {
    def onOpen(handshakedata: ServerHandshake) {
      log info s"Opened websocket to: $uri"
      connectPromise.trySuccess(())
      //      subject onNext SocketConnected
    }

    def onMessage(message: String): Unit = {
      //      info(s"Message: $message")
      val json = Json parse message
      JsonWebSocketClient.this.onMessage(json)
      subject onNext json
    }

    /**
     *
     * @param code 1000 if the client disconnects normally, 1006 if the server dies abnormally
     * @param reason
     * @param remote
     * @see http://tools.ietf.org/html/rfc6455#section-7.4.1
     */
    def onClose(code: Int, reason: String, remote: Boolean) {
      log info s"Closed websocket to: $uri, code: $code, reason: $reason, remote: $remote"
      connectPromise.tryFailure(new NotConnectedException(s"The websocket was closed. Code: $code, reason: $reason."))
      JsonWebSocketClient.this.onClose()
      //      eventsSubject onNext Disconnected
      subject.onCompleted()
    }

    /**
     * Exceptions thrown in this handler like in onMessage end up here.
     *
     * If the connection attempt fails, this is called with a [[java.net.ConnectException]].
     */
    def onError(ex: Exception) {
      log.warn("WebSocket error", ex)
      connectPromise.tryFailure(ex)
      JsonWebSocketClient.this.onError(ex)
      subject onError ex
    }
  }
  if (uri startsWith "wss") {
    // makes SSL-encrypted websockets work with self-signed server certificates
    val factory = new DefaultSSLWebSocketClientFactory(SSLUtils.trustAllSslContext())
    client setWebSocketFactory factory
  }

  def close(): Unit = client.close()

  override def onClose(): Unit = ()

  override def onError(t: Exception): Unit = ()

  def isConnected = client.getConnection.isOpen

  /**
   * Reconnections are currently not supported; only call this method once per instance.
   *
   * Impl: On subsequent calls, the returned future will always completed regardless of connection result
   *
   * @return a future that completes when the connection has successfully been established
   */
  def connect(): Future[Unit] = {
    client.connect()
    connectPromise.future
  }

  def send[T](message: T)(implicit writer: Writes[T]): Unit = send(Json toJson message)

  def send(json: JsValue): Unit = {
    val payload = Json stringify json
    client send payload
//    log debug s"Sent: $payload"
  }

  def onMessage(json: JsValue) = ()
}

object JsonWebSocketClient {

  trait SocketEvent

  case class SocketMessage(json: JsValue) extends SocketEvent

  case object Connecting extends SocketEvent

  case object Connected extends SocketEvent

}

