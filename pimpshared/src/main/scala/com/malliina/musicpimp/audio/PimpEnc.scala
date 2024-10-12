package com.malliina.musicpimp.audio

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.text.Normalizer

import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}
import org.apache.commons.codec.digest.DigestUtils
import play.utils.UriEncoding

object PimpEnc:
  val UTF8 = "UTF-8"

  def normalize(input: String) = Normalizer
    .normalize(input, Normalizer.Form.NFD)
    .replaceAll("[^\\p{ASCII}]", "")

  def makeIdentifier(input: String) =
    UriEncoding.encodePathSegment(normalize(input.replace('\\', '/')), StandardCharsets.UTF_8)

  def encode(in: String) = URLEncoder.encode(in, UTF8)

  def decode(in: String) = URLDecoder.decode(in, UTF8)

  def decodeId(trackID: Identifier) = URLDecoder.decode(trackID.id, UTF8)

  def double(in: String): String = encode(decode(in))

  def folder(in: FolderID) = if in.id.length == 32 then in else FolderID(idFor(decodeId(in)))

  def track(in: TrackID) = if in.id.length == 32 then in else TrackID(idFor(decodeId(in)))

  def idFor(in: String) = DigestUtils.md5Hex(in)
