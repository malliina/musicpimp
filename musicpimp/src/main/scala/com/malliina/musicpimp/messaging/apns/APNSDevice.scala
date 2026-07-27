package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.{ServerTag, TaggedDevice}
import com.malliina.push.apns.APNSToken
import io.circe.Codec

case class APNSDevice(id: APNSToken, tag: ServerTag) extends TaggedDevice[APNSToken]
  derives Codec.AsObject
