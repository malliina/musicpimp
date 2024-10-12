package com.malliina.musicpimp.stats

import com.malliina.values.Username
import io.circe.{Codec, Json}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{QueryStringBindable, RequestHeader}

/** @param username
  *   relevant user
  * @param from
  *   start index of results, inclusive
  * @param until
  *   end index, exclusive
  */
case class DataRequest(username: Username, from: Int, until: Int) derives Codec.AsObject:
  val maxItems = math.max(0, until - from)

object DataRequest:
  val DefaultItemCount = ItemLimits.DefaultItemCount
  val From = "from"
  val Until = "until"
  val Page = "page"
  val PageSize = "pageSize"

  val binder = QueryStringBindable.bindableInt

  def default(user: Username): DataRequest =
    apply(user, DefaultItemCount)

  def apply(user: Username, items: Int): DataRequest =
    apply(user, 0, items)

  def fromRequest(request: AuthenticatedRequest[?, Username]): Either[String, DataRequest] =
    fromRequest(request.user, request)

  def fromRequest(user: Username, request: RequestHeader): Either[String, DataRequest] =
    ItemLimits.fromRequest(request) map { limits => DataRequest(user, limits.from, limits.until) }

  def fromJson(user: Username, body: Json) =
    ItemLimits.fromJson(body) map { limits => DataRequest(user, limits.from, limits.until) }
