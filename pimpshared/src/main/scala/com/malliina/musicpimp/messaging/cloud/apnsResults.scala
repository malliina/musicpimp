package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.apns.{APNSError, APNSIdentifier, APNSToken}
import io.circe.Codec
import play.api.libs.json.{Json, OFormat}

/** @see
  *   ApnsNotification
  */
case class APNSResult(identifier: Int, expiryDate: Int)

object APNSResult:
  implicit val json: OFormat[APNSResult] = Json.format[APNSResult]

//  def fromAPNS(apns: ApnsNotification) = APNSResult(apns.getIdentifier, apns.getExpiry)

case class APNSHttpResult(token: APNSToken, id: Option[APNSIdentifier], error: Option[APNSError])

object APNSHttpResult:
  implicit val json: Codec[APNSHttpResult] = Codec.derived[APNSHttpResult]
