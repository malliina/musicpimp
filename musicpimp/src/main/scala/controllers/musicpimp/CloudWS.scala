package controllers.musicpimp

import com.malliina.musicpimp.auth.Auths

object CloudWS:
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"

  val sessionAuth = Auths.session

//class CloudWS(clouds: Clouds, ctx: ActorExecution):
//  val sockets =
//    new MediatorSockets[AuthedRequest](Props(new CloudMediator(clouds)), CloudWS.sessionAuth, ctx)
//
//  def openSocket = sockets.newSocket
