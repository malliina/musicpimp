package com.mle.musicpimp.json

import play.api.libs.json.JsValue
import com.mle.musicpimp.actor.ServerPlayerManager
import com.mle.util.Log

/**
 *
 * @author mle
 */
trait JsonSendah extends Log{
  def send(json: JsValue) {
    log debug s"Event: $json"
    ServerPlayerManager.king ! json
  }
}
object JsonSendah extends JsonSendah
