package com.malliina.audio.meta

import java.nio.file.Path
import org.jaudiotagger.audio.{AudioFile, AudioFileIO}
import org.jaudiotagger.tag.FieldKey

case class SongTags(title: String, album: String, artist: String)

object SongTags {
  def fromFilePath(path: Path, root: Path) = {
    val relativePath = root relativize path
    val maybeParent = Option(relativePath.getParent)
    val title = SongMeta.titleFromFileName(path)
    val album = maybeParent.flatMap(p => Option(p.getFileName).map(_.toString)).getOrElse("")
    // Both getParent and getFileName may return null. Thanks, Java.
    val artist = maybeParent.map(p => {
      Option(p.getParent).map(pp => {
        Option(pp.getFileName).map(_.toString).getOrElse(album)
      }).getOrElse(album)
    }).getOrElse(album)
    SongTags(title, artist, album)
  }

  def fromTags(media: Path) = fromAudioFile(AudioFileIO read media.toFile)

  /**
   *
   * @param audio file meta
   * @return ID tags wrapped in an [[scala.Option]], or [[scala.None]] if no tags are found or if the title tag is empty even if found
   */
  def fromAudioFile(audio: AudioFile) = {
    Option(audio.getTag).map(tags => {
      def field(key: FieldKey) = tags getFirst key
      SongTags(field(FieldKey.TITLE), field(FieldKey.ALBUM), field(FieldKey.ARTIST))
    }).filter(_.title.nonEmpty)
  }
}