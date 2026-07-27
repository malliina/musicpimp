package com.malliina.audio

import scala.concurrent.duration.Duration

trait IPlayer[F[_]] extends AutoCloseable:
  /** Starts or resumes playback, whichever makes sense.
    */
  def play(): F[Unit]

  /** Pauses playback.
    */
  def stop(): F[Unit]

  /** Seeks to `pos`.
    *
    * @param pos
    *   position to seek to
    */
  def seek(pos: Duration): F[Unit]

  /** Adjusts the volume.
    *
    * @param level
    *   [0, 100]
    */
  def volume(level: Int): F[Unit]

  /** Mutes/unmutes the player.
    *
    * @param mute
    *   true to mute, false to unmute
    */
  def mute(mute: Boolean): F[Unit]

  def toggleMute(): F[Unit]

  /** Releases any player resources (input streams, ...). Playback is stopped.
    */
  def close(): Unit
