package com.malliina.musicpimp

import io.circe.Codec

case class BuildMeta(name: String, version: String, scalaVersion: String, gitHash: String)
  derives Codec.AsObject

object BuildMeta:
  def default =
    BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.gitHash)
