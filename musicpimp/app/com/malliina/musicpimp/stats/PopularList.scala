package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json}

case class PopularList(populars: Seq[PopularEntry])

object PopularList {
  implicit def json(implicit f: Format[TrackMeta]) = Json.format[PopularList]
}
