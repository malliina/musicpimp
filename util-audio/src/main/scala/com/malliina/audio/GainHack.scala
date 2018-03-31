package com.malliina.audio

import javax.sound.sampled.FloatControl

trait GainHack extends RichPlayer {
  def gainControl: FloatControl

  private lazy val maxDbGain = gainControl.getMaximum
  private lazy val minDbGain = gainControl.getMinimum
  private lazy val zeroGain = 0.4f
  private lazy val maxGain = 1.0f
  private lazy val posDbFactor = maxDbGain / (maxGain - zeroGain)
  private lazy val negDbFactor = zeroGain / -minDbGain

  def dbValue(gainLevel: Float) = {
    if (gainLevel >= zeroGain) {
      (gainLevel - zeroGain) * posDbFactor
    } else {
      (zeroGain - gainLevel) * minDbGain / zeroGain
    }
  }

  def gainValue(dbLevel: Float) = {
      if (dbLevel >= 0f) {
        zeroGain + dbLevel / posDbFactor
      } else {
        (-minDbGain + dbLevel) * negDbFactor
      }
  }
//  abstract override def gain = gainValue(super.gain)
}
