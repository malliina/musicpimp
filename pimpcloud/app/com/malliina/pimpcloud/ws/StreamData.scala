package com.malliina.pimpcloud.ws

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.play.ContentRange
import play.api.libs.json.Json

case class StreamData(uuid: RequestID,
                      serverID: CloudID,
                      track: Track,
                      range: ContentRange)

object StreamData {
  implicit val format = Json.format[StreamData]
}
