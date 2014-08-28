package com.mle.musicpimp.db

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class DataTrack(id: String, artist: String, album: String, track: String)

object DataTrack {
  implicit val format = Json.format[DataTrack]
}