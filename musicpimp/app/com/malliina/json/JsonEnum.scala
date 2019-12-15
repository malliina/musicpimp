package com.malliina.json

import play.api.libs.json._

trait JsonEnum[T] {
  def all: Seq[T]

  def resolveName(item: T): String

  def withName(name: String): Option[T] =
    all.find(i => resolveName(i).toLowerCase == name.toLowerCase)

  implicit object jsonFormat extends Format[T] {
    def allNames = all.map(resolveName).mkString(", ")

    override def reads(json: JsValue): JsResult[T] =
      json
        .validate[String]
        .flatMap(n =>
          withName(n)
            .map(tu => JsSuccess(tu))
            .getOrElse(JsError(s"Unknown name: $n. Must be one of: $allNames."))
        )

    override def writes(o: T): JsValue = Json.toJson(resolveName(o))
  }

}
