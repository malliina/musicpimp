package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings
import com.malliina.musicpimp.models.*
import io.circe.Encoder

class MusicItems extends BaseScript with FrontStrings:
  installHandler(TrackClass, PlayClass)(id => PlayTrack(TrackID(id)))
  installHandler(TrackClass, AddClass)(id => AddTrack(TrackID(id)))
  installHandler(FolderClass, PlayClass)(id => PlayItems.folder(FolderID(id)))
  installHandler(FolderClass, AddClass)(id => AddItems.folder(FolderID(id)))

  private def installHandler[C: Encoder](cls: String, more: String*)(toMessage: String => C): Unit =
    withDataId(cls, more*)(id => postPlayback(toMessage(id)))

  private def postPlayback[C: Encoder](json: C) =
    postAjax("/playback", json)
