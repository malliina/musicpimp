package com.mle.musicpimp.audio

import com.mle.audio.javasound.JavaSoundPlayer
import com.mle.audio.meta.{SongTags, StreamInfo}

/**
 *
 * @author mle
 */
class PimpJavaSoundPlayer(val track: TrackMeta)
  extends JavaSoundPlayer(StreamInfo(track.stream, track.duration, track.size))
  with PimpPlayer {
  override val tags = SongTags(track.title, track.album, track.artist)
  //  val meta = track.meta
}