package com.malliina.musicpimp.beam

import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.musicpimp.models.TrackID
import com.malliina.values.{Password, Username}
import io.circe.Codec

case class BeamCommand(track: TrackID, uri: FullUrl, username: Username, password: Password)
  extends PimpMessage derives Codec.AsObject
