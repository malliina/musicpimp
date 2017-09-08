package com.malliina.musicpimp.audio

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.text.Normalizer

import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}
import play.utils.UriEncoding

object PimpEnc {
  val UTF8 = "UTF-8"

  def normalize(input: String) = Normalizer
    .normalize(input, Normalizer.Form.NFD)
    .replaceAll("[^\\p{ASCII}]", "")

  def makeIdentifier(input: String) = {
    UriEncoding.encodePathSegment(normalize(input.replace('\\', '/')), StandardCharsets.UTF_8)
  }

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
