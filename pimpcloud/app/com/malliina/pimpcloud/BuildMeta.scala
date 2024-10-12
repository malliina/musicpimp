package com.malliina.pimpcloud

import com.malliina.play.Writeables
import play.api.http.Writeable
import play.api.libs.json.{Json, OFormat}

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)

object BuildMeta:
  implicit val json: OFormat[BuildMeta] = Json.format[BuildMeta]
  implicit val html: Writeable[BuildMeta] = Writeables.fromJson[BuildMeta]

  def default =
    BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
