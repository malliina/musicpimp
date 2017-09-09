package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import play.api.libs.json.Json

case class PopularList(populars: Seq[FullPopularEntry])

object PopularList {
  implicit val json = Json.format[PopularList]

  def forEntries(populars: Seq[PopularEntry], host: FullUrl): PopularList =
    PopularList(populars.map(_.toFull(host)))
}
