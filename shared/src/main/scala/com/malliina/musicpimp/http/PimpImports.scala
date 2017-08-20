package com.malliina.musicpimp.http

import com.malliina.musicpimp.models.{FolderID, FolderIDs, TrackID, TrackIDs}
import play.api.mvc.PathBindable

object PimpImports {
  implicit val trackBindable: PathBindable[TrackID] = TrackIDs.bindable
  implicit val folderBindable: PathBindable[FolderID] = FolderIDs.bindable
}
