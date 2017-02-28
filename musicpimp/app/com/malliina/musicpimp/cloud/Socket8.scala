package com.malliina.musicpimp.cloud

import java.net.URI
import java.util
import javax.net.ssl.SSLSocketFactory

import com.malliina.musicpimp.cloud.Socket8.log
import com.malliina.play.http.FullUrl
import com.malliina.ws.{NotConnectedException, WebSocketBase}
import com.neovisionaries.ws.client._
import play.api.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

object Socket8 {
  private val log = Logger(getClass)
}

abstract class Socket8[T](val uri: FullUrl, socketFactory: SSLSocketFactory, headers: (String, String)*)
  extends WebSocketBase[T] {

  protected val connectPromise = Promise[Unit]()
  val connectTimeout = 20.seconds

  val factory = new WebSocketFactory()
  factory.setSSLSocketFactory(socketFactory)
  val socket = factory.createSocket(uri.url, connectTimeout.toMillis.toInt)
  headers foreach {
    case (key, value) => socket.addHeader(key, value)
  }
  socket.addListener(new WebSocketAdapter {
    override def onConnected(websocket: WebSocket, headers: util.Map[String, util.List[String]]): Unit = {
      connectPromise.trySuccess(())
      Socket8.this.onConnect(websocket.getURI)
    }

    override def onTextMessage(websocket: WebSocket, text: String): Unit = {
      Socket8.this.onRawMessage(text)
    }

    override def onDisconnected(websocket: WebSocket,
                                serverCloseFrame: WebSocketFrame,
                                clientCloseFrame: WebSocketFrame,
                                closedByServer: Boolean): Unit = {
      val uri = websocket.getURI
      val suffix = if (closedByServer) " by the server" else ""
      connectPromise tryFailure new NotConnectedException(s"The websocket to $uri was closed$suffix.")
      Socket8.this.onClose()
    }

    override def onError(websocket: WebSocket, cause: WebSocketException): Unit = {
      log.error(s"Websocket error for ${websocket.getURI.toString}", cause)
      connectPromise tryFailure cause
      Socket8.this.onError(cause)
    }
  })

  override def connect(): Future[Unit] = {
    Try(socket.connectAsynchronously()) match {
      case Success(_) =>
        connectPromise.future
      case Failure(t) =>
        connectPromise tryFailure t
        connectPromise.future
    }
  }

  protected def parse(raw: String): Option[T]

  protected def stringify(message: T): String

  def onMessage(message: T): Unit = ()

  protected def onRawMessage(raw: String) = parse(raw).map(onMessage) getOrElse {
    log.warn(s"Unable to parse message: $raw")
  }

  def onConnect(uri: URI): Unit = ()

  override def onError(t: Exception): Unit = ()

  override def onClose(): Unit = ()

  def close(): Unit = socket.disconnect()

  def isConnected = socket.isOpen

  override def send(json: T): Try[Unit] = Try(socket.sendText(stringify(json)))
}
