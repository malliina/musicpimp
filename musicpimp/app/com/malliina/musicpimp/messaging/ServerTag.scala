package com.malliina.musicpimp.messaging

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat

case class ServerTag(tag: String)

object ServerTag extends SimpleCompanion[String, ServerTag] {
  override def raw(t: ServerTag): String = t.tag
}
