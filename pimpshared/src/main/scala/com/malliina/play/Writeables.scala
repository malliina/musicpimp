package com.malliina.play

import com.malliina.json.SharedPlayFormats
import io.circe.Encoder
import play.api.http.Writeable
import play.api.libs.json.{Json, Writes}

object Writeables:
  def fromJson[J: Writes]: Writeable[J] =
    Writeable.writeableOf_JsValue.map[J](Json.toJson(_))

  def fromCirceJson[J: Encoder]: Writeable[J] =
    Writeable.writeableOf_JsValue.map[J](SharedPlayFormats.writer[J].writes(_))
