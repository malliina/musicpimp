package com.malliina.musicpimp.stats

import play.api.libs.json.Json

case class PopularList(populars: Seq[PopularEntry])

object PopularList {
  implicit val json = Json.format[PopularList]
}
