package com.malliina.musicpimp.audio

import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{Path, Paths}

import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}

object PimpEnc {
  val UTF8 = "UTF-8"

  def relativePath(itemId: Identifier): Path = Paths get decode(itemId)

  /** Generates a URL-safe ID of the given music item.
    *
    * TODO: make item unique
    *
    * @param path path to music file or folder
    * @return the id
    */
  def encodePath(path: Path) = encode(path.toString)

  def encodeFolder(path: Path) = FolderID(encodePath(path))

  def encodeTrack(path: Path) = TrackID(encodePath(path))

  def encode(id: String) = URLEncoder.encode(id, UTF8)

  def decode(trackID: Identifier) = URLDecoder.decode(trackID.id, UTF8)
}
