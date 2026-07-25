package com.malliina.audio

import cats.Applicative

trait StateAwarePlayer[F[_]: Applicative] extends IPlayer[F]:
  def state: PlayerStates.PlayerState
  def onEndOfMedia(): F[Unit] = Applicative[F].unit
