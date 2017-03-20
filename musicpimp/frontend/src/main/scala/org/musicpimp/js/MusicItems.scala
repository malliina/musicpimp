package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings

class MusicItems extends BaseScript with FrontStrings {
  installHandler(s".$TrackClass.$PlayClass", TrackCommand.play)
  installHandler(s".$TrackClass.$AddClass", TrackCommand.add)
  installHandler(s".$FolderClass.$PlayClass", ItemsCommand.playFolder)
  installHandler(s".$FolderClass.$AddClass", ItemsCommand.addFolder)

  def installHandler[C: PimpJSON.Writer](clazzSelector: String, toMessage: String => C) =
    withDataId(clazzSelector)(id => postPlayback(toMessage(id)))

  def postPlayback[C: PimpJSON.Writer](json: C) =
    postAjax("/playback", json)
}
