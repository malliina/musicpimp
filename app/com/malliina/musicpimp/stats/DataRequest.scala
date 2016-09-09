package com.malliina.musicpimp.stats

import com.malliina.musicpimp.models.User
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.QueryStringBindable
import play.api.mvc.Security.AuthenticatedRequest

/**
  *
  * @param username relevant user
  * @param from     start index of results, inclusive
  * @param until    end index, exclusive
  */
case class DataRequest(username: User, from: Int, until: Int) {
  val maxItems = math.max(0, until - from)
}

object DataRequest {
  implicit val json = Json.format[DataRequest]

  val DefaultItemCount = 100
  val From = "from"
  val Until = "until"
  val Page = "page"
  val PageSize = "pageSize"

  val binder = QueryStringBindable.bindableInt

  def default(user: User): DataRequest =
    apply(user, DefaultItemCount)

  def apply(user: User, items: Int): DataRequest =
    apply(user, 0, items)

  def fromRequest(request: AuthenticatedRequest[_, User]): Either[String, DataRequest] = {
    val queryString = request.queryString
    def readInt(key: String, default: Int) =
      (binder.bind(key, queryString) getOrElse Right(default)).right
    for {
      from <- readInt(From, 0)
      until <- readInt(Until, from + DefaultItemCount)
    } yield DataRequest(request.user, from, until)
  }

  def fromJson(user: User, body: JsValue): JsResult[DataRequest] = {
    def readInt(key: String, default: Int) =
      (body \ key).validateOpt[Int].map(_ getOrElse default)
    for {
      from <- readInt(From, 0)
      until <- readInt(Until, from + DefaultItemCount)
    } yield DataRequest(user, from, until)
  }
}
