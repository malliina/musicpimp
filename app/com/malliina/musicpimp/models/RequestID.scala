package com.malliina.musicpimp.models

case class RequestID(id: String) {
  override def toString = id
}

object RequestID extends SimpleCompanion[String, RequestID] {
  override def raw(t: RequestID): String = t.id
}
