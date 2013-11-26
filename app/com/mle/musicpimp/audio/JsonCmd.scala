package com.mle.musicpimp.audio

import play.api.libs.json.JsValue
import com.mle.musicpimp.json.JsonStrings._

/**
 *
 * @author mle
 */
class JsonCmd(json: JsValue) {
  val command = (json \ CMD).as[String]

  def value = (json \ VALUE).as[Int]

  def boolValue = (json \ VALUE).as[Boolean]

  def stringValue = (json \ VALUE).as[String]

  def track = (json \ TRACK).as[String]
}
