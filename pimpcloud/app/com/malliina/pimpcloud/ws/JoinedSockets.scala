package com.malliina.pimpcloud.ws

import akka.actor.Props
import com.malliina.pimpcloud.auth.ProdAuth
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.ws.{ActorConfig, Sockets}
import controllers.pimpcloud.{PhoneConnection, PimpAuth, Servers, UsageStreaming}

class JoinedSockets(pimpAuth: PimpAuth, ctx: ActorExecution) {
  val phoneMediator = ctx.actorSystem.actorOf(Props(new PhoneMediator))
  val servers = new Servers(phoneMediator, ctx)
  val auths = new ProdAuth(servers)
  val phoneAuth = Authenticator(auths.authPhone)
  val phones = new Sockets(phoneAuth, ctx) {
    override def props(conf: ActorConfig[PhoneConnection]) =
      Props(new PhoneActor(phoneMediator, conf))
  }
  val us = new UsageStreaming(phoneMediator, servers.serverMediator, pimpAuth, ctx)

  def serverSocket = servers.serverSockets.newSocket

  def phoneSocket = phones.newSocket
}
