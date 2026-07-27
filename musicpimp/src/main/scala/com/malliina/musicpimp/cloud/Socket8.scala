package com.malliina.musicpimp.cloud

import cats.effect.{Async, Deferred}
import cats.effect.std.Dispatcher
import cats.implicits.{catsSyntaxFlatMapOps, toFunctorOps}

import java.net.URI
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocketFactory
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.Socket8.log
import com.malliina.util.AppLogger
import com.malliina.ws.{NotConnectedException, WebSocketBase}
import com.neovisionaries.ws.client.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Socket8:
  private val log = AppLogger(getClass)

abstract class Socket8[F[_]: Async, T](
  val uri: FullUrl,
  connectPromise: Deferred[F, Option[Throwable]],
  d: Dispatcher[F],
  socketFactory: SSLSocketFactory,
  headers: (String, String)*
) extends WebSocketBase[F, T]:
  val F = Async[F]
  // For some reason, onDisconnected() is called even when the socket has never been connected.
  // This variable is used to get rid of redundant "disconnected" events.
  private val hasBeenConnected = new AtomicBoolean(false)
  val connectTimeout = 20.seconds

  val factory = new WebSocketFactory()
  factory.setSSLSocketFactory(socketFactory)
//  factory.setVerifyHostname(false)
  val socket = factory.createSocket(uri.url, connectTimeout.toMillis.toInt)
  headers foreach { case (key, value) =>
    socket.addHeader(key, value)
  }
  private val adapter = new WebSocketAdapter:
    override def onConnected(
      websocket: WebSocket,
      headers: util.Map[String, util.List[String]]
    ): Unit =
      log.info(s"Connected to $uri.")
      hasBeenConnected.set(true)
      d.unsafeRunSync(connectPromise.complete(None))
      Socket8.this.onConnect(websocket.getURI)

    override def onTextMessage(websocket: WebSocket, text: String): Unit =
      Socket8.this.onRawMessage(text)

    override def onDisconnected(
      websocket: WebSocket,
      serverCloseFrame: WebSocketFrame,
      clientCloseFrame: WebSocketFrame,
      closedByServer: Boolean
    ): Unit =
      log.info(s"Disconnected from $uri.")
      if hasBeenConnected.get() then
        val uri = websocket.getURI
        val suffix = if closedByServer then " by the server" else ""
        val e = new NotConnectedException(s"The websocket to $uri was closed$suffix.")
        connectPromise.complete(Option(e))
        Socket8.this.onClose()

    override def onError(websocket: WebSocket, cause: WebSocketException): Unit =
      log.error(s"Websocket error for ${websocket.getURI.toString}", cause)
      connectPromise.complete(Option(cause))
      Socket8.this.onError(cause)
  socket.addListener(adapter)

  override def connect(): F[Unit] =
    log.info(s"Attempting to connect to $uri.")
    Try(socket.connectAsynchronously()) match
      case Success(_) =>
        getOrError(connectPromise)
      case Failure(t) =>
        connectPromise.complete(Option(t)) >> getOrError(connectPromise)

  private def getOrError(d: Deferred[F, Option[Throwable]]): F[Unit] =
    d.get.map: opt =>
      opt.fold(()): t =>
        Async[F].raiseError(t)

  protected def parse(raw: String): Option[T]
  protected def stringify(message: T): String
  def onMessage(message: T): F[Unit] = F.unit

  private def onRawMessage(raw: String): Unit = parse(raw)
    .map(msg => d.unsafeRunAndForget(onMessage(msg)))
    .getOrElse:
      log.warn(s"Unable to parse message: $raw")

  def onConnect(uri: URI): Unit = ()
  override def onError(t: Exception): F[Unit] = F.unit
  override def onClose(): F[Unit] = F.unit
  def close(): Unit = socket.disconnect()
  def isConnected = socket.isOpen

  /** Sends a message to the server. Disconnects if it fails, since perhaps it means the connection
    * is dead and a reconnect should be attempted.
    */
  override def send(json: T): Try[Unit] =
    val asString = stringify(json)
    Try:
      socket.sendText(asString)
      ()
    .recover:
      case t =>
        log.error(s"Unable to send message '$asString' over '$uri'. Disconnecting...", t)
        close()
        ()
