package org.musicpimp.js

class MusicItems extends BaseScript {
  installHandler(".track.play", TrackCommand.play)
  installHandler(".track.add", TrackCommand.add)
  installHandler(".folder.play", ItemsCommand.playFolder)
  installHandler(".folder.add", ItemsCommand.addFolder)

  def installHandler[C: PimpJSON.Writer](clazzSelector: String, toMessage: String => C) =
    withDataId(clazzSelector)(id => postPlayback(toMessage(id)))

  def postPlayback[C: PimpJSON.Writer](json: C) =
    postAjax("/playback", json)
}
