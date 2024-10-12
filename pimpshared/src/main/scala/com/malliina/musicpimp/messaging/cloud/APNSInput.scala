package com.malliina.musicpimp.messaging.cloud

import io.circe.Codec

case class APNSInput(messages: Seq[APNSPayload]) derives Codec.AsObject
