package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import play.api.libs.json.Json

case class RecentList(recents: Seq[FullRecentEntry])

object RecentList {
  implicit val json = Json.format[RecentList]

  def forEntries(recents: Seq[RecentEntry], host: FullUrl): RecentList =
    RecentList(recents.map(_.toFull(host)))
}
