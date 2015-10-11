package com.mle.play

import play.api.http.Writeable
import play.api.libs.json.{Json, Writes}

/**
 * @author mle
 */
object Writeables {
  def fromJson[J: Writes]: Writeable[J] = Writeable.writeableOf_JsValue.map[J](Json.toJson(_))
}
