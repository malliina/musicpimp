package com.malliina.audio.javasound

/** Mix this in to [[JavaSoundPlayer]]s when you want to close the audio stream when the player is
  * closed. Whether you want to do that depends on who creates the stream.
  */
trait SourceClosing[F[_]] extends JavaSoundPlayer[F]:
  abstract override def close(): Unit =
    super.close()
    stream.close()
