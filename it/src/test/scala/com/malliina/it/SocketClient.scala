package com.malliina.it

import java.io.Closeable
import java.net.URI
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{Executors, TimeUnit}
import javax.net.ssl.SSLSocketFactory

import com.malliina.it.SocketClient.{DefaultConnectTimeout, log}
import com.neovisionaries.ws.client._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}

object SocketClient {
  private val log = Logging(getClass)

  val DefaultConnectTimeout = 20.seconds
}

/** A WebSocket client.
  *
  * Creating an instance of this class will open and maintain a WebSocket to `uri`.
  *
  * Supports automatic reconnections. Calling `close()` will close any open resources
  * and cancel future reconnections, after which this instance must no longer be used.
  */
class SocketClient(val uri: URI,
                   socketFactory: SSLSocketFactory,
                   headers: Seq[KeyValue],
                   connectTimeout: FiniteDuration = DefaultConnectTimeout) extends Closeable {
  private val enabled = new AtomicBoolean(true)
  private val connected = new AtomicBoolean(false)
  // polls for connectivity, reconnects if necessary
  private val loopExecutor = Executors.newSingleThreadScheduledExecutor()
  private val loop = loopExecutor.scheduleWithFixedDelay(new Runnable {
    override def run() = ensureConnected()
  }, 30, 30, TimeUnit.SECONDS)

  private val sf = new WebSocketFactory
  sf setSSLSocketFactory socketFactory
  sf setConnectionTimeout 5.seconds.toMillis.toInt

  private val firstConnection = Promise[URI]()

  // The listener seems stateless, so it is safe to reuse it across connections
  private val listener = new WebSocketAdapter {

    override def handleCallbackError(websocket: WebSocket, cause: Throwable) = super.handleCallbackError(websocket, cause)

    override def onConnected(websocket: WebSocket,
                             headers: util.Map[String, util.List[String]]) = {
      log info s"Connected to ${websocket.getURI}."
      connected set true
      firstConnection trySuccess websocket.getURI
    }

    override def onConnectError(websocket: WebSocket, exception: WebSocketException) = {
      log.error(s"Connect error to ${websocket.getURI}.", exception)
      firstConnection tryFailure exception
    }

    override def onTextMessage(websocket: WebSocket, text: String) = {
      onText(text)
    }

    override def onDisconnected(websocket: WebSocket,
                                serverCloseFrame: WebSocketFrame,
                                clientCloseFrame: WebSocketFrame,
                                closedByServer: Boolean) = {
      log warn s"Disconnected from ${websocket.getURI}."
      connected set false
      firstConnection tryFailure new Exception(s"Disconnected from ${websocket.getURI}.")
    }

    // may fire multiple times; onDisconnected fires just once
    override def onError(websocket: WebSocket, cause: WebSocketException) = {
      log.error(s"Socket ${websocket.getURI} failed.", cause)
    }
  }

  private val socket = new AtomicReference[WebSocket](createNewSocket())

  def onText(message: String): Unit = {}

  def send(message: String) = socket.get().sendText(message)

  def initialConnection: Future[URI] = firstConnection.future

  def isConnected: Boolean = connected.get()

  def isEnabled: Boolean = enabled.get()

  private def createNewSocket(): WebSocket = {
    val socket: WebSocket = sf.createSocket(uri, connectTimeout.toSeconds.toInt)
    headers foreach { header => socket.addHeader(header.key, header.value) }
    socket addListener listener
    log info s"Connecting to $uri..."
    socket.connectAsynchronously()
  }

  private def ensureConnected() = {
    if (isEnabled) {
      if (!isConnected) {
        reconnect()
      }
    } else {
      log warn s"Socket to $uri is no longer enabled."
    }
  }

  private def reconnect(): Unit = {
    killSocket(socket.get())
    socket set createNewSocket()
  }

  private def killSocket(victim: WebSocket): Unit = {
    victim removeListener listener
    victim.disconnect()
  }

  override def close() = {
    loop cancel true
    loopExecutor.shutdown()
    loopExecutor.awaitTermination(2, TimeUnit.SECONDS)
    enabled set false
    killSocket(socket.get())
  }
}
