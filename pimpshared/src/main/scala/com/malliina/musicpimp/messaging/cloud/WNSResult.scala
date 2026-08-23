package com.malliina.musicpimp.messaging.cloud

import io.circe.Codec

case class WNSResult(reason: String, description: String, statusCode: Int, isSuccess: Boolean)
  derives Codec.AsObject
