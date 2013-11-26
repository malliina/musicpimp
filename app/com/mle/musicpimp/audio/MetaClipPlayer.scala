package com.mle.musicpimp.audio

import com.mle.audio.meta.SongMeta
import com.mle.audio.clip.ClipPlayer

/**
 * @author Michael
 */
class MetaClipPlayer(val meta: SongMeta)
  extends ClipPlayer(meta.media.uri)
  with PimpPlayer{
  def status: StatusEvent17 = StatusEvent17.empty
}