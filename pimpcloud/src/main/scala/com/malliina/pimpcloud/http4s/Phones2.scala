package com.malliina.pimpcloud.http4s

import com.malliina.http4s.BasicService
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.TrackID
import com.malliina.storage.StorageSize
import org.http4s.Header
import org.http4s.headers.{`Content-Disposition`, `Content-Length`}
import org.typelevel.ci.CIStringSyntax

import java.nio.file.Paths

object Phones2:
  val Bytes = "bytes"
  val DefaultSearchLimit = 100

  val `Accept-Ranges` = ci"Accept-Ranges"

  def trackHeaders(fileName: String, size: StorageSize): Seq[Header.ToRaw] = Seq(
    Header.Raw(`Accept-Ranges`, Bytes),
    BasicService.noCache,
    `Content-Disposition`("inline", Map(ci"filename" -> fileName)),
    `Content-Length`(size.toBytes)
  )

  def name(t: Track, id: TrackID): String =
    Option(Paths.get(t.path.path).getFileName).map(_.toString).getOrElse(id.id)
