package com.malliina.audio

import scala.concurrent.duration.Duration

trait IPlayer extends AutoCloseable:
  /** Starts or resumes playback, whichever makes sense.
    */
  def play(): Unit

  /** Pauses playback.
    */
  def stop(): Unit

  /** Seeks to `pos`.
    *
    * @param pos
    *   position to seek to
    */
  def seek(pos: Duration): Unit

  /** Adjusts the volume.
    *
    * @param level
    *   [0, 100]
    */
  def volume(level: Int): Unit

  /** Mutes/unmutes the player.
    *
    * @param mute
    *   true to mute, false to unmute
    */
  def mute(mute: Boolean): Unit

  def toggleMute(): Unit

  /** Releases any player resources (input streams, ...). Playback is stopped.
    */
  def close(): Unit
