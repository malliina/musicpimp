package com.malliina.musicpimp.stats

import play.api.libs.json.Json

case class RecentList(recents: Seq[RecentEntry])

object RecentList {
  implicit val json = Json.format[RecentList]
}
