package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import controllers.musicpimp.routes
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call

abstract class ListLike[T <: TopEntry](val entries: Seq[T], baseCall: Call) {
  val prevOffset = math.max(0, meta.from - meta.maxItems)
  val nextOffset = meta.from + meta.maxItems

  def meta: DataRequest

  def username = meta.username

  def from = meta.from

  def until = meta.until

  def prev = withOffset(prevOffset)

  def next = withOffset(nextOffset)

  def withOffset(offset: Int) = baseCall.copy(url = baseCall.url + s"?from=$offset")
}

case class PopularList(meta: DataRequest, populars: Seq[FullPopularEntry])
  extends ListLike(populars, routes.Website.popular)

object PopularList {
  implicit val json: OFormat[PopularList] = Json.format[PopularList]

  def forEntries(meta: DataRequest, populars: Seq[PopularEntry], host: FullUrl): PopularList =
    PopularList(meta, populars.map(_.toFull(host)))
}

case class RecentList(meta: DataRequest, recents: Seq[FullRecentEntry])
  extends ListLike(recents, routes.Website.recent)

object RecentList {
  implicit val json: OFormat[RecentList] = Json.format[RecentList]

  def forEntries(meta: DataRequest, recents: Seq[RecentEntry], host: FullUrl): RecentList =
    RecentList(meta, recents.map(_.toFull(host)))
}
