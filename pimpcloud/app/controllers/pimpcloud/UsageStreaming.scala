package controllers.pimpcloud

import com.malliina.play.ActorExecution
import com.malliina.play.ws.{Mediator, MediatorSockets}

class UsageStreaming(auth: PimpAuth, ctx: ActorExecution) {
  //  val jsonEvents: Observable[JsValue] = servers.usersJson merge phoneSockets.usersJson merge servers.uuidsJson
  val sockets = new MediatorSockets(Mediator.props(), auth, ctx)
  val mediator = sockets.mediator

  def openSocket = sockets.newSocket
}
