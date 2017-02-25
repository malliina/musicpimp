package com.malliina.pimpcloud

import akka.actor.{ActorRef, Props}
import com.malliina.pimpcloud.auth.ProdAuth
import com.malliina.pimpcloud.ws.{PhoneActor, PhoneMediator}
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.ws.{ActorConfig, Sockets}
import controllers.pimpcloud.{PhoneConnection, Servers}

class JoinedSockets(updates: ActorRef, ctx: ActorExecution) {
  val phoneMediator = ctx.actorSystem.actorOf(Props(new PhoneMediator(updates)))
  val servers = new Servers(updates, phoneMediator, ctx)
  val auths = new ProdAuth(servers)
  val phoneAuth = Authenticator(auths.authPhone)
  val phones = new Sockets(phoneAuth, ctx) {
    override def props(conf: ActorConfig[PhoneConnection]) =
      Props(new PhoneActor(phoneMediator, conf))
  }

  def serverSocket = servers.serverSockets.newSocket

  def phoneSocket = phones.newSocket
}
