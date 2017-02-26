package com.malliina.it

import scala.language.implicitConversions

case class KeyValue(key: String, value: String)

object KeyValue {
  implicit def fromTuple(t: (String, String)): KeyValue =
    KeyValue(t._1, t._2)
}
