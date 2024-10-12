package com.malliina.musicpimp.json

import io.circe.Json

trait Target:
  def send(json: Json): Unit

object Target:
  val noop = Target(_ => ())

  def apply(execute: Json => Unit): Target =
    (json: Json) => execute(json)
