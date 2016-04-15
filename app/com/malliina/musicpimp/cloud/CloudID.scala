package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.SimpleCompanion

case class CloudID(id: String)

object CloudID extends SimpleCompanion[String, CloudID] {
  val empty = CloudID("")

  override def raw(t: CloudID): String = t.id
}
