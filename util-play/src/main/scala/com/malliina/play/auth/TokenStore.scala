package com.malliina.play.auth

import com.malliina.values.Username

trait TokenStore[F[_]]:
  def persist(token: Token): F[Unit]
  def remove(token: Token): F[Unit]
  def removeAll(user: Username): F[Unit]
  def remove(user: Username, series: Long): F[Unit]
  def findToken(user: Username, series: Long): F[Option[Token]]
