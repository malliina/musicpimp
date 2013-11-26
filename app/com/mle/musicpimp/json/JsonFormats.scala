package com.mle.musicpimp.json

/**
 * Clients specify the desired response format, including version, in the `Accept` HTTP header.
 *
 * This trait contains the supported JSON formats.
 *
 * Adapted from http://developer.github.com/v3/media/
 *
 * @author mle
 */
trait JsonFormats {
  val JSONv17 = versionString(Some(17))
  val JSONv18 = versionString(Some(18))
  val anyJson = versionString(None)
  val latest = JSONv18

  private def versionString(maybeVersion: Option[Int]) = {
    val versionPart = maybeVersion map (v => s".v$v") getOrElse ""
    s"application/vnd.musicpimp$versionPart+json"
  }

}

object JsonFormats extends JsonFormats