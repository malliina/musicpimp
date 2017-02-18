package com.malliina.ws

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import com.malliina.play.http.AuthedRequest
import com.malliina.ws.ServerSocket.log
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import rx.lang.scala.subjects.BehaviorSubject

abstract class ServerSocket(val storage: RxStmStorage[PimpServerSocket], val mat: Materializer)
  extends ServerActorSockets(mat) {

  override type Client = PimpServerSocket
  override type AuthSuccess = AuthedRequest
  val subject = BehaviorSubject[SocketEvent](Users(Nil))

  def openSocketCall: Call

  def updateRequestList(): Unit

  override def newClient(authResult: AuthedRequest, channel: SourceQueue[JsValue], request: RequestHeader): PimpServerSocket =
    new PimpServerSocket(channel, CloudID(authResult.user.name), request, mat, updateRequestList)

  override def onConnectSync(client: PimpServerSocket): Unit = {
    super.onConnectSync(client)
    subject onNext Connected(client)
    log info s"Server '${client.id}' connected."
  }

  override def onDisconnectSync(client: PimpServerSocket): Unit = {
    super.onDisconnectSync(client)
    subject onNext Disconnected(client)
    log info s"Server '${client.id}' disconnected."
  }

  trait SocketEvent

  case class Users(users: Seq[PimpServerSocket]) extends SocketEvent

  case class Connected(client: PimpServerSocket) extends SocketEvent

  case class Disconnected(client: PimpServerSocket) extends SocketEvent

}

object ServerSocket {
  private val log = Logger(getClass)
}
