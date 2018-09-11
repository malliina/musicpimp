package com.malliina.musicmeta

import play.api.libs.json.Json

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)

object BuildMeta {
  implicit val json = Json.format[BuildMeta]

  def default = BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
}
