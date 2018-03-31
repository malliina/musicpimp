package com.malliina.audio.javasound

/** Mix this in to [[JavaSoundPlayer]]s when you want to close the audio stream
  * when the player is closed. Whether you want to do that depends on who creates
  * the stream.
  */
trait SourceClosing extends JavaSoundPlayer {
  abstract override def close() {
    super.close()
    stream.close()
  }
}
