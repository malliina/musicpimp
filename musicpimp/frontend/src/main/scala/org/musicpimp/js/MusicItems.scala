package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings
import com.malliina.musicpimp.models._
import play.api.libs.json.Writes

class MusicItems extends BaseScript with FrontStrings {
  installHandler(s".$TrackClass.$PlayClass", id => PlayTrack(TrackID(id)))
  installHandler(s".$TrackClass.$AddClass", id => AddTrack(TrackID(id)))
  installHandler(s".$FolderClass.$PlayClass", id => PlayItems.folder(FolderID(id)))
  installHandler(s".$FolderClass.$AddClass", id => AddItems.folder(FolderID(id)))

  def installHandler[C: Writes](clazzSelector: String, toMessage: String => C) =
    withDataId(clazzSelector)(id => postPlayback(toMessage(id)))

  def postPlayback[C: Writes](json: C) =
    postAjax("/playback", json)
}
