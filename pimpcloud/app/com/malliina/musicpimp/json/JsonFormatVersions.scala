package com.malliina.musicpimp.json

/** Clients specify the desired response format, including version, in the `Accept` HTTP header.
  *
  * This trait contains the supported JSON formats.
  *
  * Adapted from http://developer.github.com/v3/media/
  */
trait JsonFormatVersions {
  val JSONv17 = versionString(Some(17))
  val JSONv18 = versionString(Some(18))
  //  /**
  //   * JSONv18 diff JSONv24:
  //   * key: playlist_index -> index, but playlist_index also remains
  //   *
  //   * Clients accepting v18 are compatible with v24 responses.
  //   */
  //  val JSONv24 = versionString(Some(24))
  val anyJson = versionString(None)
  val latest = JSONv18

  private def versionString(maybeVersion: Option[Int]) = {
    val versionPart = maybeVersion.fold("")(v => s".v$v")
    s"application/vnd.musicpimp$versionPart+json"
  }

}

object JsonFormatVersions extends JsonFormatVersions