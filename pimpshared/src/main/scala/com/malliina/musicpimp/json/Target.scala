package com.malliina.musicpimp.json

import cats.effect.Sync
import io.circe.Json

trait Target[F[_]]:
  def send(json: Json): F[Unit]

object Target:
  def noop[F[_]: Sync] = Target(_ => Sync[F].unit)

  def apply[F[_]](execute: Json => F[Unit]): Target[F] =
    (json: Json) => execute(json)
