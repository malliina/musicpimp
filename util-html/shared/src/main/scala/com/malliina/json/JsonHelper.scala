package com.malliina.json

import play.api.libs.json._

object JsonHelper {
  def defaultFormat[T, U: Writes](read: JsValue => JsResult[T], write: T => U): Format[T] =
    format(read, t => Json.toJson(write(t)))

  def format[T](read: JsValue => JsResult[T], write: T => JsValue): Format[T] =
    Format(Reads(read), Writes(write))
}
