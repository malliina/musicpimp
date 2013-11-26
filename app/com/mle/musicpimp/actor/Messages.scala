package com.mle.musicpimp.actor

import play.api.libs.json.JsValue
import play.api.libs.iteratee.Concurrent.Channel

/**
 * @author Michael
 */
object Messages {

  case class JsonMessage(json: JsValue)

  case object Restart

  case object StartIfStopped

  case object Shutdown

  case class UserJson(user: String, json: JsValue)

  case class ChannelJson(channel: Channel[JsValue], json: JsValue)

}
