package com.malliina.play

import play.api.http.Writeable
import play.api.libs.json.{Json, Writes}

object Writeables {
  def fromJson[J: Writes]: Writeable[J] =
    Writeable.writeableOf_JsValue.map[J](Json.toJson(_))
}
