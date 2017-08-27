package com.malliina.musicpimp.json

import play.api.libs.json.JsValue

trait Target {
  def send(json: JsValue): Unit
}

object Target {
  val noop = Target(_ => ())

  def apply(execute: JsValue => Unit): Target =
    (json: JsValue) => execute(json)
}
