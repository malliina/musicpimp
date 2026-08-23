package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.apns.{APNSError, APNSIdentifier, APNSToken}
import io.circe.Codec

/** @see
  *   ApnsNotification
  */
case class APNSResult(identifier: Int, expiryDate: Int) derives Codec.AsObject

case class APNSHttpResult(token: APNSToken, id: Option[APNSIdentifier], error: Option[APNSError])
  derives Codec.AsObject
