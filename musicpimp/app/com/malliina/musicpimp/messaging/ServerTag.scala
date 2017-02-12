package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.models.SimpleCompanion

case class ServerTag(tag: String)

object ServerTag extends SimpleCompanion[String, ServerTag] {
  override def raw(t: ServerTag): String = t.tag
}
