package com.mle.musicpimp.audio

import com.mle.audio.{StateAwarePlayer, RichPlayer}
import com.mle.audio.meta.SongMeta

/**
 * @author Michael
 */
trait MetaPlayer
  extends RichPlayer
  with StateAwarePlayer {

  def meta: SongMeta
}
