package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.Json

/**
  * @author mle
  */
case class PushTask(apns: Option[APNSRequest],
                    gcm: Option[GCMRequest],
                    adm: Option[ADMRequest],
                    mpns: Option[MPNSRequest])

object PushTask {
  implicit val json = Json.format[PushTask]
}
