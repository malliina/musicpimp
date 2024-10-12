package com.malliina.pimpcloud.ws

import org.apache.pekko.actor.Props
import com.malliina.pimpcloud.auth.ProdAuth
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.ws.{ActorConfig, Sockets}
import controllers.pimpcloud.{PimpAuth, Servers, UsageStreaming}
import play.api.http.HttpErrorHandler

class JoinedSockets(pimpAuth: PimpAuth, ctx: ActorExecution, errorHandler: HttpErrorHandler):
  val phoneMediator = ctx.actorSystem.actorOf(Props(new PhoneMediator))
  val servers = new Servers(phoneMediator, ctx, errorHandler)
  val auths = new ProdAuth(servers, errorHandler)
  val phoneAuth = Authenticator(rh => auths.authPhone(rh, errorHandler))
  val phones = new Sockets(phoneAuth, ctx):
    override def props(conf: ActorConfig[PhoneConnection]) =
      Props(new PhoneActor(phoneMediator, conf))
  val us = new UsageStreaming(phoneMediator, servers.serverMediator, pimpAuth, ctx)

  def serverSocket = servers.serverSockets.newSocket
  def phoneSocket = phones.newSocket
