package com.malliina.musicpimp.models

import com.malliina.play.PlayString
import slick.jdbc.H2Profile.api.{MappedColumnType, longColumnType, stringColumnType}

object FolderIDs extends PlayString[FolderID] {
  implicit val folderData = MappedColumnType.base[FolderID, String](raw, apply)

  override def apply(raw: String) = FolderID(raw)

  override def raw(t: FolderID) = t.id
}
