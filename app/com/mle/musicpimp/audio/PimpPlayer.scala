package com.mle.musicpimp.audio

import com.mle.audio.{RichPlayer, StateAwarePlayer}

/**
 * @author Michael
 */
trait PimpPlayer
  extends StateAwarePlayer
  with RichPlayer
  with MetaPlayer {
}