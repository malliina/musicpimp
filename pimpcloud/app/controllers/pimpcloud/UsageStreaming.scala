package controllers.pimpcloud

import org.apache.pekko.actor.{ActorRef, Props}
import com.malliina.pimpcloud.ws.ServerMediator.Listen
import com.malliina.play.ActorExecution
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws._

class UsageStreaming(phones: ActorRef, servers: ActorRef, auth: PimpAuth, ctx: ActorExecution) {
  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]): Props =
      Props(new AdminActor(phones, servers, conf))
  }

  def openSocket = sockets.newSocket
}

class AdminActor(phones: ActorRef, servers: ActorRef, ctx: ActorMeta) extends JsonActor(ctx) {
  override def preStart(): Unit = {
    super.preStart()
    servers ! Listen(out)
    phones ! Listen(out)
  }
}
