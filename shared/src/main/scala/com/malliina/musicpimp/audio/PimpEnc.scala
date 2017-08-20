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
  def encode(path: Path) = URLEncoder.encode(path.toString, UTF8)

  def encodeFolder(path: Path) = FolderID(encode(path))

  def encodeTrack(path: Path) = TrackID(encode(path))

  def decode(trackID: Identifier) = URLDecoder.decode(trackID.id, UTF8)
}
