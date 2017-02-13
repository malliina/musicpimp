package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.Json

case class PushTask(apns: Option[APNSRequest],
                    gcm: Option[GCMRequest],
                    adm: Option[ADMRequest],
                    mpns: Option[MPNSRequest],
                    wns: Option[WNSRequest])

object PushTask {
  implicit val json = Json.format[PushTask]
}
