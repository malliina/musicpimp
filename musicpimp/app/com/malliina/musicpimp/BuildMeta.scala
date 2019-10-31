package com.malliina.musicpimp

import com.malliina.play.Writeables
import play.api.libs.json.Json

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)

object BuildMeta {
  implicit val json = Json.format[BuildMeta]
  implicit val html = Writeables.fromJson[BuildMeta]

  def default =
    BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
}
