package com.malliina.pimpcloud.ws

import java.util.UUID

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.CloudID
import com.malliina.play.ContentRange
import play.api.libs.json.Json

case class StreamData(uuid: UUID,
                      serverID: CloudID,
                      track: Track,
                      range: ContentRange)

object StreamData {
  implicit val format = Json.format[StreamData]
}
