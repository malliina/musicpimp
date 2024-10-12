package com.malliina.musicpimp.models

import com.malliina.play.PlayString

object FolderIDs extends PlayString[FolderID]:
  override def apply(raw: String) = FolderID(raw)

  override def raw(t: FolderID) = t.id
