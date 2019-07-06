package com.malliina.musicpimp.stats

import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.{QueryStringBindable, RequestHeader}

case class ItemLimits(from: Int, until: Int)

object ItemLimits {
  implicit val json = Json.format[ItemLimits]

  val DefaultItemCount = 300
  val From = "from"
  val Until = "until"

  val Default = ItemLimits(0, DefaultItemCount)

  val intBinder = QueryStringBindable.bindableInt

  def fromRequest(request: RequestHeader): Either[String, ItemLimits] = {
    val queryString = request.queryString
    def readInt(key: String, default: Int) =
      intBinder.bind(key, queryString) getOrElse Right(default)
    for {
      from <- readInt(From, 0)
      until <- readInt(Until, from + DefaultItemCount)
    } yield ItemLimits(from, until)
  }

  def fromJson(body: JsValue): JsResult[ItemLimits] = {
    def readInt(key: String, default: Int) =
      (body \ key).validateOpt[Int].map(_ getOrElse default)
    for {
      from <- readInt(From, 0)
      until <- readInt(Until, from + DefaultItemCount)
    } yield ItemLimits(from, until)
  }
}
