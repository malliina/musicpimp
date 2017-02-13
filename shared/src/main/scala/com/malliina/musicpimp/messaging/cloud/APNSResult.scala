package com.malliina.musicpimp.messaging.cloud

import com.notnoop.apns.ApnsNotification
import play.api.libs.json.Json

/**
  * @see ApnsNotification
  */
case class APNSResult(identifier: Int, expiryDate: Int)

object APNSResult {
  implicit val json = Json.format[APNSResult]

  def fromAPNS(apns: ApnsNotification) = APNSResult(apns.getIdentifier, apns.getExpiry)
}
