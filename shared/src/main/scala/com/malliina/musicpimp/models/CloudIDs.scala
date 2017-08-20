package com.malliina.musicpimp.models

object CloudIDs extends IDCompanion[CloudID] {
  override def apply(raw: String): CloudID = CloudID(raw)
}
