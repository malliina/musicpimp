package com.malliina.pimpcloud

import scala.concurrent.Future

trait AsyncClientHandler[M, C] {
  def onConnect(c: C): Future[Unit]

  def onDisconnect(c: C): Future[Unit]

  def broadcast(m: M): Future[Unit]

  def clients: Future[Set[C]]
}
