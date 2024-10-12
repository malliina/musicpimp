package com.malliina.play.auth

import com.malliina.values.Username

import scala.concurrent.{ExecutionContext, Future}

trait TokenStore {
  implicit def ec: ExecutionContext

  def persist(token: Token): Future[Unit]

  def remove(token: Token): Future[Unit]

  def removeAll(user: Username): Future[Unit]

  def remove(user: Username, series: Long): Future[Unit]

  def findToken(user: Username, series: Long): Future[Option[Token]]
}
