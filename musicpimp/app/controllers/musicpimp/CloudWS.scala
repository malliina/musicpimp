package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.play.ActorExecution
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.MediatorSockets

object CloudWS {
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"

  val sessionAuth = Auths.session
}

class CloudWS(clouds: Clouds, ctx: ActorExecution) {
  val sockets =
    new MediatorSockets[AuthedRequest](Props(new CloudMediator(clouds)), CloudWS.sessionAuth, ctx)

  def openSocket = sockets.newSocket
}
