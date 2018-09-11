package com.malliina.beam

import play.api.http.Writeable
import play.api.libs.json.Json

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)

object BuildMeta {
  implicit val json = Json.format[BuildMeta]
  implicit val html = Writeable.writeableOf_JsValue.map[BuildMeta](Json.toJson(_))

  def default = BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
}
