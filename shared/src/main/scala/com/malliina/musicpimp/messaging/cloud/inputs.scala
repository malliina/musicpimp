package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.adm.ADMToken
import com.malliina.push.android.AndroidMessage
import com.malliina.push.apns.{APNSMessage, APNSToken}
import com.malliina.push.gcm.{GCMMessage, GCMToken}
import com.malliina.push.mpns.MPNSToken
import com.malliina.push.wns.WNSToken
import play.api.libs.json.{Json, OFormat}

case class APNSPayload(token: APNSToken, message: APNSMessage)

case object APNSPayload {
  implicit val json: OFormat[APNSPayload] = Json.format[APNSPayload]
}

case class GCMPayload(token: GCMToken, message: GCMMessage)

object GCMPayload {
  implicit val json: OFormat[GCMPayload] = Json.format[GCMPayload]
}

case class ADMPayload(token: ADMToken, message: AndroidMessage)

object ADMPayload {
  implicit val json: OFormat[ADMPayload] = Json.format[ADMPayload]
}

case class MPNSPayload(token: MPNSToken, message: MPNSRequest)

object MPNSPayload {
  implicit val json: OFormat[MPNSPayload] = Json.format[MPNSPayload]
}

case class WNSPayload(token: WNSToken, message: WNSRequest)

object WNSPayload {
  implicit val json: OFormat[WNSPayload] = Json.format[WNSPayload]
}
