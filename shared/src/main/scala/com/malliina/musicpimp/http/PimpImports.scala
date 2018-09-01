package com.malliina.musicpimp.http

import com.malliina.musicpimp.models._
import com.malliina.play.http.Bindables
import com.malliina.values.Username
import play.api.mvc.PathBindable

object PimpImports {
  implicit val trackBindable: PathBindable[TrackID] = TrackIDs.bindable
  implicit val folderBindable: PathBindable[FolderID] = FolderIDs.bindable
  implicit val userBindable: PathBindable[Username] = Bindables.username
}
