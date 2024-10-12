package com.malliina.beam

import com.malliina.http.PlayCirce
import io.circe.Codec
import play.api.http.Writeable

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)
  derives Codec.AsObject

object BuildMeta:
  implicit val html: Writeable[BuildMeta] = PlayCirce.jsonWriteable[BuildMeta]

  def default =
    BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
