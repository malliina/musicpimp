package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json}

case class RecentList(recents: Seq[RecentEntry])

object RecentList {
  implicit def json(implicit f: Format[TrackMeta]): Format[RecentList] =
    Json.format[RecentList]
}
