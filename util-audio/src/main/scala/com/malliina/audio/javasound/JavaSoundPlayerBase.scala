package com.malliina.audio.javasound

import javax.sound.sampled.{BooleanControl, Control, FloatControl, SourceDataLine}

import com.malliina.audio.RichPlayer
import com.malliina.audio.javasound.JavaSoundPlayerBase.log
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

trait JavaSoundPlayerBase extends RichPlayer with Seekable {
  protected def audioLine: SourceDataLine

  private val zeroGain = 0.4f
  private val maxGain = 1.0f

  private var volumeCache: Option[Int] = None
  private var muteCache: Option[Boolean] = None

  def cachedVolume = volumeCache

  def cachedMute = muteCache

  /**
    * Bug: DataLine.getMicrosecondPosition returns milliseconds on Oracle's ARM JVM.
    *
    * To workaround the bug, this method conditionally converts what may be milliseconds
    * to microseconds. However, that's based on guessing, so when the position is incorrectly
    * reported as milliseconds and the position is over 1000 seconds, no conversion takes
    * place and milliseconds are incorrectly returned.
    *
    * Better implementations are welcome.
    *
    * TODO if(sys.props("os.arch").contains("arm")) hack else donthack
    *
    * @return microseconds since the line was opened
    */
  //  private def microsSinceLineOpened4 = {
  //    val microsOrMillis = audioLine.getMicrosecondPosition
  //    val multiplier = if (microsOrMillis > 1000 && microsOrMillis < 1000000) 1000L else 1L
  //    multiplier * microsOrMillis
  //  }

  // Method getLongFramePosition exists since 1.5.
  // So is the tritonus-share library compiled against something earlier than 1.4 or why
  // does IntelliJ complain here?
  private def microsSinceLineOpened = framesToMicroseconds(audioLine.getLongFramePosition)

  //  private def microsSinceLineOpened2 = {
  //    val frameSizeBytes = audioLine.getFormat.getFrameSize
  //    val positionInBytes = audioLine.getLongFramePosition * frameSizeBytes
  //    (1.0 * positionInBytes / media.size.toBytes) * media.duration.toMicros
  //  }

  // http://stackoverflow.com/questions/9470148/how-do-you-play-a-long-audioclip see if this formula works better
  private def framesToMicroseconds(frames: Long): Long =
    (frames / audioLine.getFormat.getSampleRate.toLong) * 1000000L

  def position: Duration = (startedFromMicros + microsSinceLineOpened).micros

  def canAdjustVolume = hasVolumeControl || hasGainControl

  def volume: Int =
    if (hasVolumeControl) volumeControlValue
    else if (hasGainControl) (gainControlValue * 100).toInt
    else {
      val cached = cachedVolume.getOrElse(0)
      log.info(s"Unable to find volume/gain control. Returning volume $cached.")
      cached
    }

  def volume_=(newVolume: Int): Unit = {
    if (hasVolumeControl) volumeControlValue(newVolume)
    else if (hasGainControl) gainControlValue(1.0F * newVolume / 100)
    else {
      log.info("Cannot set volume, because no volume control was found.")
      // should I throw an exception?
    }
    volumeCache = Some(newVolume)
  }

  // implements trait
  override def volume(newVolume: Int): Unit = volume = newVolume

  def gainControl = control[FloatControl](FloatControl.Type.MASTER_GAIN)

  def volumeControl = control[FloatControl](FloatControl.Type.VOLUME)

  def hasGainControl = hasControl(FloatControl.Type.MASTER_GAIN)

  def hasVolumeControl = hasControl(FloatControl.Type.VOLUME)

  /**
    * Adjusts the volume.
    *
    * @param level [0.0F, 1.0F]
    */
  private def gainControlValue(level: Float): Unit =
    gainControl foreach (c => c.setValue(dbValue(level, c)))

  /**
    * @return [0.0F, 1.0F]
    */
  private def gainControlValue =
    gainControl.map(gainValue).getOrElse {
      log.info(s"Unable to find gain control; returning 0f as gain.")
      0F
    }

  /**
    * (currentVolume-min) / (max-min) = x
    *
    * @return [0, 100]
    */
  private def volumeControlValue: Int =
    volumeControl.map(externalVolumeValue).getOrElse {
      log.info("Unable to find volume control; returning 0 as volume")
      0
    }

  /**
    * Sets the volume.
    *
    * @param newVolume [0, 100]
    */
  private def volumeControlValue(newVolume: Int): Unit =
    volumeControl.foreach(ctrl => {
      val newValue = internalVolumeValue(newVolume, ctrl.getMinimum, ctrl.getMaximum)
      ctrl setValue newValue
    })

  def internalVolumeValue(newVolume: Int, min: Float, max: Float): Float =
    min + 1.0F * newVolume / 100 * (max - min)

  private def externalVolumeValue(volumeControl: FloatControl): Int =
    externalVolumeValue(volumeControl.getValue, volumeControl.getMinimum, volumeControl.getMaximum)

  def externalVolumeValue(internalValue: Float, min: Float, max: Float): Int = {
    val volumePercentage = (internalValue - min) / (max - min)
    (volumePercentage * 100).toInt
  }

  def muteControl = control[BooleanControl](BooleanControl.Type.MUTE)

  def mute(shouldMute: Boolean): Unit = {
    muteControl.foreach(c => c.setValue(shouldMute))
    muteCache = Some(shouldMute)
  }

  def mute = muteControl.exists(_.getValue)

  def toggleMute(): Unit = mute(!mute)

  private def hasControl(ctrl: Control.Type) =
    Option(audioLine).exists(_.isControlSupported(ctrl))

  /**
    * This throws an [[IllegalArgumentException]] after end of media. So I think that if the
    * audioline is closed, getting a supported control may still throw [[IllegalArgumentException]].
    *
    * Thus this method is unsafe, wrap in Try() or whathaveyou.
    *
    * @param controlType type of control: volume, mute, ...
    * @tparam T actual type of control
    * @return the control
    */
  private def control[T](controlType: Control.Type): Option[T] = Option(audioLine)
    .filter(_.isControlSupported(controlType))
    .map(_.getControl(controlType).asInstanceOf[T])

  /**
    *
    * @param gainControl
    * @return [0.0F, 1.0F]
    */
  private def gainValue(gainControl: FloatControl) = {
    val maxDbGain = gainControl.getMaximum
    val minDbGain = gainControl.getMinimum
    val posDbFactor = maxDbGain / (maxGain - zeroGain)
    val negDbFactor = zeroGain / -minDbGain
    val dbLevel = gainControl.getValue
    if (dbLevel >= 0f) {
      zeroGain + dbLevel / posDbFactor
    } else {
      (-minDbGain + dbLevel) * negDbFactor
    }
  }

  /**
    *
    * @param normalizedValue [0.0F, 1.0F]
    * @param gainControl
    * @return db value
    */
  private def dbValue(normalizedValue: Float, gainControl: FloatControl) = {
    val maxDbGain = gainControl.getMaximum
    val minDbGain = gainControl.getMinimum
    val posDbFactor = maxDbGain / (maxGain - zeroGain)
    if (normalizedValue >= zeroGain) {
      (normalizedValue - zeroGain) * posDbFactor
    } else {
      (zeroGain - normalizedValue) * minDbGain / zeroGain
    }
  }
}

object JavaSoundPlayerBase {
  private val log = LoggerFactory.getLogger(getClass)
}
