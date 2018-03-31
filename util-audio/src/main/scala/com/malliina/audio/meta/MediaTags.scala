package com.malliina.audio.meta

import java.nio.file.Path
import org.jaudiotagger.audio.AudioFileIO
import scala.concurrent.duration._

object MediaTags {
  def audioDuration(media: Path): Duration = {
    val f = AudioFileIO read media.toFile
    f.getAudioHeader.getTrackLength.toDouble.seconds
  }
}
