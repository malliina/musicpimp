package com.malliina.musicpimp.messaging.gcm

import com.malliina.musicpimp.messaging.{ServerTag, TaggedDevice}
import com.malliina.push.gcm.GCMToken
import io.circe.Codec

case class GCMDevice(id: GCMToken, tag: ServerTag) extends TaggedDevice[GCMToken]
  derives Codec.AsObject
