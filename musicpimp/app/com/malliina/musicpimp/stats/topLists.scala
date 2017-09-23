package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import play.api.libs.json.Json

trait ListLike {
  def meta: DataRequest

  def username = meta.username
}

case class PopularList(meta: DataRequest, populars: Seq[FullPopularEntry]) extends ListLike

object PopularList {
  implicit val json = Json.format[PopularList]

  def forEntries(meta: DataRequest, populars: Seq[PopularEntry], host: FullUrl): PopularList =
    PopularList(meta, populars.map(_.toFull(host)))
}

case class RecentList(meta: DataRequest, recents: Seq[FullRecentEntry]) extends ListLike

object RecentList {
  implicit val json = Json.format[RecentList]

  def forEntries(meta: DataRequest, recents: Seq[RecentEntry], host: FullUrl): RecentList =
    RecentList(meta, recents.map(_.toFull(host)))
}
