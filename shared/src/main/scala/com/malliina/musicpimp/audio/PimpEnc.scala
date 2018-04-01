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

  def encode(in: String) = URLEncoder.encode(in, UTF8)

  def decode(in: String) = URLDecoder.decode(in, UTF8)

  def decodeId(trackID: Identifier) = URLDecoder.decode(trackID.id, UTF8)

  def double(in: String): String = encode(decode(in))

  def folder(in: FolderID) = FolderID(double(in.id))

  def track(in: TrackID) = TrackID(double(in.id))
}
