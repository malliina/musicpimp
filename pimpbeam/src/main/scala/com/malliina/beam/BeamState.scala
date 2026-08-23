package com.malliina.beam

import cats.implicits.toFlatMapOps
import cats.effect.{Async, Ref}
import cats.implicits.toFunctorOps
import com.malliina.values.Username
import fs2.concurrent.Topic
import io.circe.Json

case class BeamMessage(message: Json, user: Username, toPlayer: Boolean):
  def toPhone = !toPlayer

object BeamState:
  def default[F[_]: Async]: F[BeamState[F]] =
    for
      phones <- Ref.of[F, Set[PhoneClient[F]]](Set.empty)
      players <- Ref.of[F, Set[PlayerClient[F]]](Set.empty)
      topic <- Topic[F, BeamMessage]
    yield BeamState(phones, players, topic)

class BeamState[F[_]: Async](
  phones: Ref[F, Set[PhoneClient[F]]],
  players: Ref[F, Set[PlayerClient[F]]],
  topic: Topic[F, BeamMessage]
):
  def messages = topic.subscribe(100)
  def send(msg: BeamMessage) = topic.publish1(msg).map(_.isRight)
  def connected(player: PlayerClient[F]) = players.updateAndGet(set => set ++ Set(player))
  def phoneConnected(phone: PhoneClient[F]) = phones.updateAndGet(set => set ++ Set(phone))
  def disconnected(player: PlayerClient[F]) =
    for
      _ <- players.updateAndGet(set => set.filterNot(_.user == player.user))
      _ <- send(BeamMessage(BeamMessages.partyDisconnected(player.user), player.user, false))
    yield ()
  def phoneDisconnected(phone: PhoneClient[F]) =
    for
      _ <- phones.updateAndGet(set => set.filterNot(_.user == phone.user))
      _ <- send(BeamMessage(BeamMessages.partyDisconnected(phone.user), phone.user, true))
    yield ()

  def find(user: Username): F[Option[PlayerClient[F]]] = players.get.map: ps =>
    ps.find(_.user == user)
