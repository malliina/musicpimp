package com.malliina.musicpimp.messaging

import com.malliina.values.JsonCompanion

case class ServerTag(tag: String)

object ServerTag extends JsonCompanion[String, ServerTag] {
  override def write(t: ServerTag): String = t.tag
}
