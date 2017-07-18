package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.Json

case class PushTask(apns: Option[APNSRequest] = None,
                    gcm: Option[GCMRequest] = None,
                    adm: Option[ADMRequest] = None,
                    mpns: Option[MPNSRequest] = None,
                    wns: Option[WNSRequest] = None) {
  def labels: Seq[String] = Seq(
    apns.map(_ => "APNS"),
    gcm.map(_ => "GCM"),
    adm.map(_ => "ADM"),
    mpns.map(_ => "MPNS"),
    wns.map(_ => "WNS")
  ).flatten
}

object PushTask {
  implicit val json = Json.format[PushTask]
}
